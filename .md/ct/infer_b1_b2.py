from __future__ import annotations

import argparse
import json
from pathlib import Path

import torch
import torch.nn.functional as F

from train_b1_b2_classifier import (
    CaseClassifier,
    apply_brain_window,
    choose_device,
    find_best_series_dir,
    read_dicom_series,
    resize_slices,
    sample_informative_slices,
)


DEFAULT_MODEL_PATH = Path("/Users/leon/PycharmProjects/neuedu/ct_demo1/b1b2_run/best_b1_b2_model.pth")
LABEL_NAMES = ["B1", "B2"]


def resolve_series_dir(input_path: Path) -> Path:
    if input_path.is_dir():
        if any(input_path.glob("*.dcm")):
            return input_path
        return find_best_series_dir(input_path)
    raise FileNotFoundError(f"输入路径必须是病例目录或 DICOM 序列目录: {input_path}")


def load_case_tensor(series_dir: Path, num_slices: int, image_size: int) -> torch.Tensor:
    volume = read_dicom_series(series_dir)
    volume = apply_brain_window(volume)
    slices = sample_informative_slices(volume, num_slices)
    slices = resize_slices(slices, image_size)
    return slices.unsqueeze(0)  # [1, S, 1, H, W]


def predict_case(
    input_path: Path,
    model_path: Path,
    num_slices: int,
    image_size: int,
) -> dict:
    device = choose_device(prefer_mps=True)
    series_dir = resolve_series_dir(input_path)

    model = CaseClassifier().to(device)
    model.load_state_dict(torch.load(model_path, map_location=device))
    model.eval()

    case_tensor = load_case_tensor(series_dir, num_slices=num_slices, image_size=image_size).to(device)

    with torch.no_grad():
        logits = model(case_tensor)
        probs = F.softmax(logits, dim=1).squeeze(0).cpu()

    pred_idx = int(torch.argmax(probs).item())
    predicted_category = LABEL_NAMES[pred_idx]
    confidence = float(probs[pred_idx].item())

    return {
        "analysisType": "B1_B2_CLASSIFICATION",
        "inputPath": str(input_path),
        "seriesDir": str(series_dir),
        "predictedCategory": predicted_category,
        "confidence": round(confidence, 6),
        "classProbabilities": {
            "B1": round(float(probs[0].item()), 6),
            "B2": round(float(probs[1].item()), 6),
        },
        "riskLevel": "HIGH" if predicted_category == "B2" else "LOW",
        "modelName": "cq500_b1_b2_classifier_v1",
        "device": str(device),
        "numSlices": num_slices,
        "imageSize": image_size,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="CQ500 B1/B2 病例级分类推理脚本")
    parser.add_argument("--input", required=True, help="病例目录或 DICOM 序列目录")
    parser.add_argument("--model", default=str(DEFAULT_MODEL_PATH), help="模型权重路径")
    parser.add_argument("--num-slices", type=int, default=16)
    parser.add_argument("--image-size", type=int, default=224)
    args = parser.parse_args()

    result = predict_case(
        input_path=Path(args.input),
        model_path=Path(args.model),
        num_slices=args.num_slices,
        image_size=args.image_size,
    )
    print(json.dumps(result, ensure_ascii=False))


if __name__ == "__main__":
    main()
