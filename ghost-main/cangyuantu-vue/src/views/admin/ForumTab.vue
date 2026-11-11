<template>
  <section class="adm-card">
    <div class="adm-h2">
      <span class="ico">❖</span>论道管理
      <span class="tip">论题 / 回帖 / 共鸣数据来自 cy_forum_* 三表</span>
      <span style="flex:1"></span>
      <button class="adm-btn sm" :disabled="loading" @click="load">{{ loading ? '加载中…' : '刷新' }}</button>
    </div>

    <div class="adm-toolbar">
      <div style="display:flex;gap:8px;flex-wrap:wrap">
        <span class="adm-tag gold">累计 {{ total }} 篇</span>
        <span class="adm-tag on">展示 {{ items.filter(i => Number(i.status) === 1).length }} / 本页</span>
        <span class="adm-tag off">隐藏 {{ items.filter(i => Number(i.status) === 0).length }} / 本页</span>
        <span class="adm-tag">置顶 {{ items.filter(i => Number(i.is_pinned) === 1).length }} / 本页</span>
      </div>
      <div style="display:flex;gap:10px;align-items:center">
        <input v-model="kw" class="adm-input" style="width:230px" placeholder="搜索论题 / 作者 / 正文…"
               @keyup.enter="doSearch" />
        <button class="adm-btn sm primary" @click="doSearch">检索</button>
      </div>
    </div>

    <div class="adm-table-wrap">
      <table class="adm-table">
        <thead>
          <tr>
            <th style="width:52px">ID</th>
            <th style="width:74px">板块</th>
            <th>论题</th>
            <th style="width:120px">发帖道友</th>
            <th style="width:120px">阅 / 赞 / 回</th>
            <th style="width:74px">置顶</th>
            <th style="width:80px">状态</th>
            <th style="width:140px">立于</th>
            <th style="width:250px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading && !items.length"><td colspan="9" class="adm-empty">正在读取论道卷宗…</td></tr>
          <tr v-else-if="!items.length"><td colspan="9" class="adm-empty">暂无论题</td></tr>
          <template v-for="p in items" :key="p.id">
            <tr>
              <td class="num">{{ p.id }}</td>
              <td><span class="adm-tag" :class="tagOf(p.category)">{{ p.category }}</span></td>
              <td class="strong" :title="p.content">{{ p.title }}</td>
              <td class="muted">{{ p.author_alias || p.author_name }}</td>
              <td class="num">{{ p.view_count || 0 }} / {{ p.like_count || 0 }} / {{ p.reply_count || 0 }}</td>
              <td><span class="adm-tag" :class="Number(p.is_pinned) === 1 ? 'gold' : 'off'">
                {{ Number(p.is_pinned) === 1 ? '置顶' : '普通' }}</span></td>
              <td><span class="adm-tag" :class="Number(p.status) === 1 ? 'on' : 'red'">
                {{ Number(p.status) === 1 ? '展示' : '隐藏' }}</span></td>
              <td class="muted">{{ fmt(p.created_at) }}</td>
              <td class="ops">
                <button class="adm-btn sm" @click="togglePin(p)">
                  {{ Number(p.is_pinned) === 1 ? '取消置顶' : '置顶' }}</button>
                <button class="adm-btn sm" :class="Number(p.status) === 1 ? 'danger' : ''" @click="toggleStatus(p)">
                  {{ Number(p.status) === 1 ? '屏蔽' : '恢复' }}</button>
                <button class="adm-btn sm" @click="openReplies(p)">回帖</button>
              </td>
            </tr>
            <tr v-if="replyOpen === p.id">
              <td colspan="9" style="background:rgba(45,212,191,.04);padding:14px 18px">
                <div style="display:flex;align-items:center;gap:12px;margin-bottom:10px">
                  <b style="color:var(--gold,#e9c46a)">《{{ p.title }}》回帖</b>
                  <span class="adm-tag">{{ replyItems.length }} 楼</span>
                  <span style="flex:1"></span>
                  <button class="adm-btn sm" @click="replyOpen = null">收起</button>
                </div>
                <div v-if="!replyItems.length" class="adm-empty" style="padding:18px">暂无回帖</div>
                <div v-for="r in replyItems" :key="r.id"
                     style="display:flex;gap:12px;align-items:flex-start;padding:9px 4px;border-bottom:1px dashed rgba(255,255,255,.08)">
                  <span class="num" style="width:38px">{{ r.floor_no }} 楼</span>
                  <b style="width:110px;flex-shrink:0">{{ r.author_alias || r.author_name }}</b>
                  <span style="flex:1;color:#c7d4e8;font-size:12.5px;line-height:1.7;white-space:pre-wrap">{{ r.content }}</span>
                  <span class="muted" style="width:130px;flex-shrink:0">{{ fmt(r.created_at) }}</span>
                  <button class="adm-btn sm danger" :disabled="Number(r.status) === 0" @click="hideReply(r, p)">
                    {{ Number(r.status) === 0 ? '已隐藏' : '隐藏' }}</button>
                </div>
              </td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>

    <div class="adm-toolbar" style="justify-content:flex-end;border-top:1px solid rgba(255,255,255,.08);margin-top:12px">
      <span class="adm-tag">第 {{ page }} / {{ Math.max(1, pages) }} 页</span>
      <button class="adm-btn sm" :disabled="page <= 1" @click="goPage(page - 1)">‹ 上一页</button>
      <button class="adm-btn sm" :disabled="page >= pages" @click="goPage(page + 1)">下一页 ›</button>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  fetchAdminForum, updateAdminForum, fetchAdminForumReplies, deleteAdminForumReply
} from '../../api'

const props = defineProps({ cred: { type: Object, required: true } })
const emit = defineEmits(['toast'])

const items = ref([])
const total = ref(0)
const page = ref(1)
const pages = ref(1)
const loading = ref(false)
const kw = ref('')
const searchKw = ref('')
const replyOpen = ref(null)
const replyItems = ref([])

async function load() {
  loading.value = true
  const d = await fetchAdminForum(props.cred, { keyword: searchKw.value, page: page.value, pageSize: 20 })
  loading.value = false
  if (d.ok) {
    items.value = Array.isArray(d.items) ? d.items : []
    total.value = Number(d.total) || 0
    page.value = Number(d.page) || 1
    pages.value = Number(d.pages) || 1
  } else emit('toast', d.msg || '论道帖加载失败', true)
}
function doSearch() { searchKw.value = kw.value.trim(); page.value = 1; load() }
function goPage(p) { page.value = Math.max(1, Math.min(pages.value, p)); replyOpen.value = null; load() }

async function togglePin(p) {
  const d = await updateAdminForum(props.cred, p.id, { pinned: Number(p.is_pinned) === 1 ? 0 : 1 })
  if (!d.ok) return emit('toast', d.msg || '操作失败', true)
  emit('toast', Number(p.is_pinned) === 1 ? '已取消置顶' : '已置顶')
  load()
}
async function toggleStatus(p) {
  const d = await updateAdminForum(props.cred, p.id, { status: Number(p.status) === 1 ? 0 : 1 })
  if (!d.ok) return emit('toast', d.msg || '操作失败', true)
  emit('toast', Number(p.status) === 1 ? '论题已屏蔽' : '论题已恢复展示')
  load()
}
async function openReplies(p) {
  if (replyOpen.value === p.id) { replyOpen.value = null; return }
  const d = await fetchAdminForumReplies(props.cred, p.id)
  if (!d.ok) return emit('toast', d.msg || '回帖加载失败', true)
  replyOpen.value = p.id
  replyItems.value = Array.isArray(d.items) ? d.items : []
}
async function hideReply(r, p) {
  const d = await deleteAdminForumReply(props.cred, r.id)
  if (!d.ok) return emit('toast', d.msg || '隐藏失败', true)
  emit('toast', '回帖已隐藏')
  const d2 = await fetchAdminForumReplies(props.cred, p.id)
  if (d2.ok) replyItems.value = Array.isArray(d2.items) ? d2.items : []
  load()
}
function tagOf(c) {
  return { '论道': 'gold', '切磋': 'on', '妖情': 'red', '闲谈': 'off' }[c] || ''
}
function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : '—' }

onMounted(load)
</script>
