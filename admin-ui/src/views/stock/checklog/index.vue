<template><stock-crud title="库存检查记录" resource="checklog" :fields="fields" :readonly="true" /></template>
<script setup lang="ts">
import StockCrud from '../components/StockCrud.vue'
const fields = [{"key": "name", "label": "产品名称", "type": "text", "required": false, "max": null}, {"key": "productId", "label": "产品ID", "type": "number", "required": false, "max": null}, {"key": "previousStatus", "label": "原状态", "type": "text", "required": false, "max": null}, {"key": "stockStatus", "label": "现状态", "type": "text", "required": false, "max": null}, {"key": "message", "label": "结果", "type": "text", "required": false, "max": null}]
</script>
