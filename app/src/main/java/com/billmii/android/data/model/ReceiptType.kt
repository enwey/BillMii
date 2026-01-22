package com.billmii.android.data.model

/**
 * Receipt type enumeration
 * 票据类型枚举
 */
enum class ReceiptType(val displayName: String, val category: ReceiptCategory) {
    // Invoice types - 发票类
    VAT_SPECIAL_INVOICE("增值税专用发票", ReceiptCategory.INVOICE),
    VAT_ORDINARY_INVOICE("增值税普通发票", ReceiptCategory.INVOICE),
    VAT_ELECTRONIC_INVOICE("增值税电子发票", ReceiptCategory.INVOICE),
    MOTOR_VEHICLE_SALES_INVOICE("机动车销售发票", ReceiptCategory.INVOICE),
    USED_CAR_SALES_INVOICE("二手车销售发票", ReceiptCategory.INVOICE),
    
    // Expense types - 费用类
    TRAIN_TICKET("火车票", ReceiptCategory.EXPENSE),
    FLIGHT_ITINERARY("飞机行程单", ReceiptCategory.EXPENSE),
    BUS_TICKET("汽车票", ReceiptCategory.EXPENSE),
    TAXI_RECEIPT("出租车票", ReceiptCategory.EXPENSE),
    ACCOMMODATION_INVOICE("住宿费发票", ReceiptCategory.EXPENSE),
    DINING_INVOICE("餐饮发票", ReceiptCategory.EXPENSE),
    
    // Other types - 其他类
    REIMBURSEMENT_FORM("报销单", ReceiptCategory.OTHER),
    RECEIPT("收据", ReceiptCategory.OTHER),
    BANK_STATEMENT("银行回单", ReceiptCategory.OTHER),
    CONTRACT_SCAN("合同扫描件", ReceiptCategory.OTHER),
    PAYROLL("工资表", ReceiptCategory.OTHER),
    EXPENSE_DETAIL("费用明细表", ReceiptCategory.OTHER),
    
    UNKNOWN("未知类型", ReceiptCategory.OTHER);

    companion object {
        fun fromString(value: String): ReceiptType {
            return values().find { it.displayName == value } ?: UNKNOWN
        }
    }
}

/**
 * Receipt category enumeration
 * 票据分类枚举
 */
enum class ReceiptCategory(val displayName: String) {
    INCOME("收入类票据"),
    EXPENSE("支出类票据"),
    EXPENSE_TYPE("费用类票据"),
    CONTRACT("合同文件"),
    VOUCHER("账务凭证"),
    OTHER("其他文件");

    companion object {
        fun fromString(value: String): ReceiptCategory {
            return values().find { it.displayName == value } ?: OTHER
        }
    }
}

/**
 * Expense sub-category enumeration
 * 费用子分类枚举
 */
enum class ExpenseSubCategory(val displayName: String) {
    PROCUREMENT("采购"),
    EXPENSE("费用"),
    ASSET("资产支出"),
    TRAVEL("差旅费"),
    OFFICE("办公费"),
    BUSINESS_ENTERTAINMENT("业务招待费"),
    WELFARE("福利费"),
    OTHER("其他");

    companion object {
        fun fromString(value: String): ExpenseSubCategory {
            return values().find { it.displayName == value } ?: OTHER
        }
    }
}