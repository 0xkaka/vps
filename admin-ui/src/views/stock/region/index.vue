<template><stock-crud title="机房库存" resource="region" :fields="fields" :readonly="false" /></template>
<script setup>
import StockCrud from "../components/StockCrud.vue"
const fields=[{"key": "productId", "label": "产品", "type": "number", "required": true, "max": null}, {"key": "name", "label": "机房名称", "type": "text", "required": true, "max": 120}, {"key": "sourceName", "label": "原站机房标识", "type": "text", "required": true, "max": 120}, {"key": "stockStatus", "label": "库存状态", "type": "text", "required": true, "max": 20}, {"key": "purchaseUrl", "label": "购买链接", "type": "text", "required": true, "max": 2000}, {"key": "sortOrder", "label": "排序", "type": "number", "required": true, "max": null}, {"key": "enabled", "label": "展示", "type": "bool", "required": true, "max": null}]
</script>