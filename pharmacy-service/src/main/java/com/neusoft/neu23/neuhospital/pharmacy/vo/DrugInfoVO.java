package com.neusoft.neu23.neuhospital.pharmacy.vo;

import java.math.BigDecimal;

public class DrugInfoVO {
    private Long id;
    private String drugCode;
    private String drugName;
    private String genericName;
    private String specification;
    private String unit;
    private String category;
    private String manufacturer;
    private BigDecimal salePrice;
    private Integer stockQuantity;
    private Integer warningQuantity;
    private String contraindication;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDrugCode() { return drugCode; }
    public void setDrugCode(String drugCode) { this.drugCode = drugCode; }

    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }

    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public Integer getWarningQuantity() { return warningQuantity; }
    public void setWarningQuantity(Integer warningQuantity) { this.warningQuantity = warningQuantity; }

    public String getContraindication() { return contraindication; }
    public void setContraindication(String contraindication) { this.contraindication = contraindication; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
