<template><stock-crud title="商家分类" resource="vendor" :fields="fields" :readonly="false" /></template>
<script setup lang="ts">
import StockCrud from '../components/StockCrud.vue'
const fields = [{"key": "name", "label": "商家名称", "type": "text", "required": true, "max": 120}, {"key": "code", "label": "商家标识", "type": "text", "required": true, "max": null}, {"key": "sortOrder", "label": "排序", "type": "number", "required": true, "max": null}, {"key": "enabled", "label": "启用", "type": "bool", "required": true, "max": null}]
</script>
