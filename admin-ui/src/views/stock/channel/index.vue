<template><stock-crud title="订阅渠道" resource="channel" :fields="fields" :readonly="false" /></template>
<script setup lang="ts">
import StockCrud from '../components/StockCrud.vue'
const fields = [{"key": "name", "label": "渠道名称", "type": "text", "required": true, "max": 120}, {"key": "type", "label": "渠道类型", "type": "text", "required": true, "max": null}, {"key": "productId", "label": "产品ID(空为全局)", "type": "number", "required": false, "max": null}, {"key": "url", "label": "订阅链接", "type": "text", "required": true, "max": 2000}, {"key": "sortOrder", "label": "排序", "type": "number", "required": true, "max": null}, {"key": "enabled", "label": "启用", "type": "bool", "required": true, "max": null}]
</script>
