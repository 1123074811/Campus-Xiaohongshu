<script setup>

import {useRoute} from "vue-router"

const route = useRoute()

const keyword = route.query.keyword
</script>

<template>
{{keyword}}
</template>

<style scoped lang="scss">

</style>