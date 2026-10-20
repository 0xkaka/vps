<template><stock-crud title="邮件订阅者" resource="subscriber" :fields="fields" :readonly="false" /></template>
<script setup lang="ts">
import StockCrud from '../components/StockCrud.vue'
const fields = [{"key": "name", "label": "邮箱", "type": "text", "required": true, "max": 254}, {"key": "status", "label": "状态", "type": "text", "required": true, "max": null}]
</script>
