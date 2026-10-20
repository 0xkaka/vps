<template><stock-crud title="导航链接" resource="link" :fields="fields" :readonly="false" /></template>
<script setup lang="ts">
import StockCrud from '../components/StockCrud.vue'
const fields = [{"key": "name", "label": "链接名称", "type": "text", "required": true, "max": 120}, {"key": "url", "label": "链接地址", "type": "text", "required": true, "max": 2000}, {"key": "position", "label": "位置", "type": "text", "required": true, "max": null}, {"key": "sortOrder", "label": "排序", "type": "number", "required": true, "max": null}, {"key": "enabled", "label": "启用", "type": "bool", "required": true, "max": null}]
</script>
