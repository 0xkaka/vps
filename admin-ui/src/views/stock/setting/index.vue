<template><stock-crud title="站点配置" resource="setting" :fields="fields" :readonly="false" /></template>
<script setup lang="ts">
import StockCrud from '../components/StockCrud.vue'
const fields = [{"key": "name", "label": "配置键", "type": "text", "required": true, "max": null}, {"key": "value", "label": "配置值", "type": "text", "required": true, "max": 2000}, {"key": "description", "label": "说明", "type": "text", "required": false, "max": 255}]
</script>
