from __future__ import annotations

import argparse
import csv
import json
import random
from dataclasses import dataclass
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import SimpleITK as sitk
import torch
import torch.nn as nn
import torch.optim as optim
import torch.nn.functional as F
from torch.utils.data import DataLoader, Dataset
import tqdm


DATA_ROOT = Path("/Users/leon/PycharmProjects/neuedu/ct_demo1/data")
READS_CSV = Path("/Users/leon/PycharmProjects/neuedu/ct_demo1/word/reads.csv")
RUN_ROOT = Path("/Users/leon/PycharmProjects/neuedu/ct_demo1/b1b2_run")

SERIES_PRIORITY = [
    "ct plain",
    "ct 5mm",
    "ct 55mm plain",
    "ct plain 3mm",
    "ct pre contrast 5mm std",
    "plain",
    "thin plain",
]

SERIES_EXCLUDE_KEYWORDS = [
    "bone",
    "contrast",
    "d3d",
    "hires",
    "vr",
]

LABEL_MAP = {"B1": 0, "B2": 1}


def seed_everything(seed: int) -> None:
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)


def choose_device(prefer_mps: bool = True) -> torch.device:
    if prefer_mps and torch.backends.mps.is_available():
        return torch.device("mps")
    if torch.cuda.is_available():
        return torch.device("cuda")
    return torch.device("cpu")


def score_series(series_name: str) -> tuple[int, int]:
    lowered = series_name.lower()
    exclude_penalty = sum(keyword in lowered for keyword in SERIES_EXCLUDE_KEYWORDS)
    for rank, keyword in enumerate(SERIES_PRIORITY):
        if keyword in lowered:
            return (exclude_penalty, rank)
    return (exclude_penalty, len(SERIES_PRIORITY))


def find_best_series_dir(case_dir: Path) -> Path:
    candidates: list[Path] = []
    for series_dir in case_dir.rglob("*"):
        if not series_dir.is_dir():
            continue
        if any(series_dir.glob("*.dcm")):
            candidates.append(series_dir)
    if not candidates:
        raise FileNotFoundError(f"病例目录中未找到 DICOM 序列: {case_dir}")
    candidates.sort(key=lambda p: score_series(p.name))
    return candidates[0]


def read_dicom_series(series_dir: Path) -> np.ndarray:
    reader = sitk.ImageSeriesReader()
    dicom_names = reader.GetGDCMSeriesFileNames(str(series_dir))
    if not dicom_names:
        raise FileNotFoundError(f"未找到 DICOM 文件: {series_dir}")
    reader.SetFileNames(dicom_names)
    image = reader.Execute()
    return sitk.GetArrayFromImage(image).astype(np.float32)


def apply_brain_window(volume: np.ndarray, width: float = 80.0, level: float = 40.0) -> np.ndarray:
    lower = level - width / 2.0
    upper = level + width / 2.0
    volume = np.clip(volume, lower, upper)
    volume = (volume - lower) / (upper - lower + 1e-6)
    return volume.astype(np.float32)


def sample_informative_slices(volume: np.ndarray, num_slices: int) -> np.ndarray:
    informative = []
    for idx in range(volume.shape[0]):
        slice_arr = volume[idx]
        foreground_ratio = float((slice_arr > 0.05).mean())
        if foreground_ratio > 0.08:
            informative.append(idx)

    if len(informative) < num_slices:
        informative = list(range(volume.shape[0]))

    positions = np.linspace(0, len(informative) - 1, num_slices, dtype=int)
    selected = [informative[pos] for pos in positions]
    return volume[selected]


def resize_slices(slices: np.ndarray, size: int) -> torch.Tensor:
    tensor = torch.from_numpy(slices).unsqueeze(1)  # [S, 1, H, W]
    tensor = F.interpolate(tensor, size=(size, size), mode="bilinear", align_corners=False)
    return tensor


def normalize_case_name(reads_name: str) -> str:
    # CQ500-CT-427 -> CQ500CT427 CQ500CT427
    suffix = reads_name.replace("CQ500-CT-", "")
    return f"CQ500CT{suffix} CQ500CT{suffix}"


@dataclass
class CaseRecord:
    case_name: str
    folder_name: str
    label_name: str
    label_id: int
    series_dir: Path


def load_case_records(data_root: Path, reads_csv: Path) -> list[CaseRecord]:
    records: list[CaseRecord] = []
    with reads_csv.open("r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        for row in reader:
            case_name = row["name"]
            category = row["Category"]
            if category not in LABEL_MAP:
                continue
            folder_name = normalize_case_name(case_name)
            case_dir = data_root / folder_name
            if not case_dir.exists():
                continue
            series_dir = find_best_series_dir(case_dir)
            records.append(
                CaseRecord(
                    case_name=case_name,
                    folder_name=folder_name,
                    label_name=category,
                    label_id=LABEL_MAP[category],
                    series_dir=series_dir,
                )
            )
    return records


def stratified_split(records: list[CaseRecord], val_ratio: float, seed: int) -> tuple[list[CaseRecord], list[CaseRecord]]:
    rng = random.Random(seed)
    grouped = {0: [], 1: []}
    for record in records:
        grouped[record.label_id].append(record)

    train_records: list[CaseRecord] = []
    val_records: list[CaseRecord] = []
    for label_id, items in grouped.items():
        rng.shuffle(items)
        val_count = max(1, int(round(len(items) * val_ratio)))
        val_records.extend(items[:val_count])
        train_records.extend(items[val_count:])

    rng.shuffle(train_records)
    rng.shuffle(val_records)
    return train_records, val_records


class CQ500CaseDataset(Dataset):
    def __init__(self, records: list[CaseRecord], num_slices: int = 16, image_size: int = 224):
        self.records = records
        self.num_slices = num_slices
        self.image_size = image_size

    def __len__(self) -> int:
        return len(self.records)

    def __getitem__(self, index: int):
        record = self.records[index]
        volume = read_dicom_series(record.series_dir)
        volume = apply_brain_window(volume)
        slices = sample_informative_slices(volume, self.num_slices)
        slices = resize_slices(slices, self.image_size)
        label = torch.tensor(record.label_id, dtype=torch.long)
        return slices, label, record.case_name


class SliceEncoder(nn.Module):
    def __init__(self, in_channels: int = 1, feature_dim: int = 128):
        super().__init__()
        self.features = nn.Sequential(
            nn.Conv2d(in_channels, 16, kernel_size=3, padding=1),
            nn.BatchNorm2d(16),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2),
            nn.Conv2d(16, 32, kernel_size=3, padding=1),
            nn.BatchNorm2d(32),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2),
            nn.Conv2d(32, 64, kernel_size=3, padding=1),
            nn.BatchNorm2d(64),
            nn.ReLU(inplace=True),
            nn.MaxPool2d(2),
            nn.Conv2d(64, feature_dim, kernel_size=3, padding=1),
            nn.BatchNorm2d(feature_dim),
            nn.ReLU(inplace=True),
            nn.AdaptiveAvgPool2d((1, 1)),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        x = self.features(x)
        return x.flatten(1)


class CaseClassifier(nn.Module):
    def __init__(self, feature_dim: int = 128, num_classes: int = 2):
        super().__init__()
        self.encoder = SliceEncoder(feature_dim=feature_dim)
        self.classifier = nn.Sequential(
            nn.Linear(feature_dim, 64),
            nn.ReLU(inplace=True),
            nn.Dropout(0.2),
            nn.Linear(64, num_classes),
        )

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        # x: [B, S, 1, H, W]
        batch_size, num_slices, channels, height, width = x.shape
        x = x.view(batch_size * num_slices, channels, height, width)
        slice_features = self.encoder(x)
        slice_features = slice_features.view(batch_size, num_slices, -1)
        case_features = slice_features.mean(dim=1)
        return self.classifier(case_features)


def compute_metrics(logits: torch.Tensor, labels: torch.Tensor) -> dict[str, float]:
    preds = torch.argmax(logits, dim=1)
    acc = float((preds == labels).float().mean().item())

    tp = float(((preds == 1) & (labels == 1)).sum().item())
    fp = float(((preds == 1) & (labels == 0)).sum().item())
    fn = float(((preds == 0) & (labels == 1)).sum().item())

    precision = tp / (tp + fp + 1e-7)
    recall = tp / (tp + fn + 1e-7)
    f1 = 2.0 * precision * recall / (precision + recall + 1e-7)
    return {"acc": acc, "precision": precision, "recall": recall, "f1": f1}


def compute_confusion_matrix(logits: torch.Tensor, labels: torch.Tensor) -> np.ndarray:
    preds = torch.argmax(logits, dim=1)
    cm = np.zeros((2, 2), dtype=np.int32)
    for truth, pred in zip(labels.cpu().numpy(), preds.cpu().numpy()):
        cm[int(truth), int(pred)] += 1
    return cm


def run_epoch(
    model: nn.Module,
    loader: DataLoader,
    device: torch.device,
    optimizer: optim.Optimizer | None,
    criterion: nn.Module,
    epoch: int,
    total_epochs: int,
) -> tuple[float, dict[str, float], torch.Tensor, torch.Tensor]:
    training = optimizer is not None
    model.train(training)

    total_loss = 0.0
    all_logits = []
    all_labels = []
    phase_name = "训练中" if training else "验证中"
    progress = tqdm.tqdm(
        loader,
        desc=f"Epoch {epoch}/{total_epochs} {phase_name}",
        leave=False,
    )

    for slices, labels, _case_names in progress:
        slices = slices.to(device)
        labels = labels.to(device)

        with torch.set_grad_enabled(training):
            logits = model(slices)
            loss = criterion(logits, labels)
            if training:
                optimizer.zero_grad()
                loss.backward()
                optimizer.step()

        total_loss += float(loss.item()) * labels.size(0)
        all_logits.append(logits.detach().cpu())
        all_labels.append(labels.detach().cpu())
        batch_acc = float((torch.argmax(logits, dim=1) == labels).float().mean().item())
        progress.set_postfix(loss=f"{loss.item():.4f}", acc=f"{batch_acc:.4f}")

    logits = torch.cat(all_logits, dim=0)
    labels = torch.cat(all_labels, dim=0)
    metrics = compute_metrics(logits, labels)
    avg_loss = total_loss / max(1, len(loader.dataset))
    return avg_loss, metrics, logits, labels


def plot_training_curves(history: list[dict], output_dir: Path) -> None:
    epochs = [item["epoch"] for item in history]

    plt.figure(figsize=(8, 5))
    plt.plot(epochs, [item["train_loss"] for item in history], label="Train Loss")
    plt.plot(epochs, [item["val_loss"] for item in history], label="Val Loss")
    plt.xlabel("Epoch")
    plt.ylabel("Loss")
    plt.title("Loss Curve")
    plt.grid(alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(output_dir / "loss_curve.png", dpi=220)
    plt.close()

    plt.figure(figsize=(8, 5))
    plt.plot(epochs, [item["train_metrics"]["acc"] for item in history], label="Train Acc")
    plt.plot(epochs, [item["val_metrics"]["acc"] for item in history], label="Val Acc")
    plt.plot(epochs, [item["train_metrics"]["f1"] for item in history], label="Train F1")
    plt.plot(epochs, [item["val_metrics"]["f1"] for item in history], label="Val F1")
    plt.xlabel("Epoch")
    plt.ylabel("Score")
    plt.title("Accuracy / F1 Curve")
    plt.grid(alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(output_dir / "metrics_curve.png", dpi=220)
    plt.close()


def plot_confusion_matrix(cm: np.ndarray, output_dir: Path) -> None:
    plt.figure(figsize=(5.5, 4.8))
    plt.imshow(cm, cmap="Blues")
    plt.title("Confusion Matrix")
    plt.xlabel("Predicted")
    plt.ylabel("True")
    plt.xticks([0, 1], ["B1", "B2"])
    plt.yticks([0, 1], ["B1", "B2"])
    for i in range(2):
        for j in range(2):
            plt.text(j, i, str(cm[i, j]), ha="center", va="center")
    plt.tight_layout()
    plt.savefig(output_dir / "confusion_matrix.png", dpi=220)
    plt.close()


def main() -> None:
    parser = argparse.ArgumentParser(description="训练 CQ500 B1/B2 病例级分类基线模型。")
    parser.add_argument("--epochs", type=int, default=12)
    parser.add_argument("--batch-size", type=int, default=4)
    parser.add_argument("--num-slices", type=int, default=16)
    parser.add_argument("--image-size", type=int, default=224)
    parser.add_argument("--val-ratio", type=float, default=0.2)
    parser.add_argument("--lr", type=float, default=1e-4)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--prefer-mps", action="store_true", default=True)
    parser.add_argument("--output-dir", type=str, default=str(RUN_ROOT))
    args = parser.parse_args()

    seed_everything(args.seed)
    sitk.ProcessObject_SetGlobalWarningDisplay(False)
    device = choose_device(prefer_mps=args.prefer_mps)

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    records = load_case_records(DATA_ROOT, READS_CSV)
    train_records, val_records = stratified_split(records, args.val_ratio, args.seed)

    print("=" * 60)
    print("CQ500 B1/B2 病例级分类训练")
    print(f"设备: {device}")
    print(f"病例总数: {len(records)}")
    print(f"训练集: {len(train_records)} | 验证集: {len(val_records)}")
    print("=" * 60)

    train_dataset = CQ500CaseDataset(train_records, num_slices=args.num_slices, image_size=args.image_size)
    val_dataset = CQ500CaseDataset(val_records, num_slices=args.num_slices, image_size=args.image_size)

    train_loader = DataLoader(train_dataset, batch_size=args.batch_size, shuffle=True, num_workers=0)
    val_loader = DataLoader(val_dataset, batch_size=args.batch_size, shuffle=False, num_workers=0)

    model = CaseClassifier().to(device)

    b1_count = sum(record.label_id == 0 for record in train_records)
    b2_count = sum(record.label_id == 1 for record in train_records)
    class_weights = torch.tensor(
        [len(train_records) / max(1, b1_count), len(train_records) / max(1, b2_count)],
        dtype=torch.float32,
        device=device,
    )
    criterion = nn.CrossEntropyLoss(weight=class_weights)
    optimizer = optim.AdamW(model.parameters(), lr=args.lr, weight_decay=1e-4)

    best_val_f1 = -1.0
    history = []

    for epoch in range(1, args.epochs + 1):
        print(f"\n======== Epoch {epoch}/{args.epochs} ========")
        train_loss, train_metrics, _train_logits, _train_labels = run_epoch(
            model, train_loader, device, optimizer, criterion, epoch, args.epochs
        )
        val_loss, val_metrics, val_logits, val_labels = run_epoch(
            model, val_loader, device, None, criterion, epoch, args.epochs
        )

        epoch_result = {
            "epoch": epoch,
            "train_loss": train_loss,
            "val_loss": val_loss,
            "train_metrics": train_metrics,
            "val_metrics": val_metrics,
        }
        history.append(epoch_result)

        print(
            f"Epoch {epoch}/{args.epochs} | "
            f"train_loss={train_loss:.4f} val_loss={val_loss:.4f} | "
            f"val_acc={val_metrics['acc']:.4f} val_f1={val_metrics['f1']:.4f}"
        )

        if val_metrics["f1"] > best_val_f1:
            best_val_f1 = val_metrics["f1"]
            torch.save(model.state_dict(), output_dir / "best_b1_b2_model.pth")
            print("  已保存当前最佳模型")

    summary = {
        "device": str(device),
        "total_cases": len(records),
        "train_cases": len(train_records),
        "val_cases": len(val_records),
        "best_val_f1": best_val_f1,
        "history": history,
        "label_map": {"B1": 0, "B2": 1},
    }
    with (output_dir / "training_summary.json").open("w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)

    plot_training_curves(history, output_dir)
    best_model_path = output_dir / "best_b1_b2_model.pth"
    if best_model_path.exists():
        model.load_state_dict(torch.load(best_model_path, map_location=device))
    final_val_loss, final_val_metrics, final_val_logits, final_val_labels = run_epoch(
        model, val_loader, device, None, criterion
    )
    confusion = compute_confusion_matrix(final_val_logits, final_val_labels)
    plot_confusion_matrix(confusion, output_dir)

    with (output_dir / "final_metrics.json").open("w", encoding="utf-8") as f:
        json.dump(
            {
                "val_loss": final_val_loss,
                "val_metrics": final_val_metrics,
                "confusion_matrix": confusion.tolist(),
            },
            f,
            ensure_ascii=False,
            indent=2,
        )

    print(f"\n训练完成，结果已保存到: {output_dir}")


if __name__ == "__main__":
    main()
