<template>
  <div class="forum" ref="root">
    <div class="embers"><i v-for="(e, i) in embers" :key="i" :style="e"></i></div>

    <!-- ===== 顶部栏 ===== -->
    <header class="nav">
      <div class="nav-inner">
        <div class="brand" @click="$emit('go-home')">
          <div class="seal">论</div>
          <div class="brand-txt"><b>论道坛</b><span>LUN DAO TAN · 元界</span></div>
        </div>
        <nav class="crumbs">
          <a @click="$emit('go-home')">首页</a><span class="sep">›</span>
          <span class="cur">论道论坛</span>
        </nav>
        <div class="nav-right">
          <span class="who" v-if="avatar">
            <i>{{ displayName.slice(0, 1) }}</i>{{ displayName }}
            <em v-if="isGuest">游客</em>
            <em v-else-if="isAdmin" class="gold">管理员</em>
          </span>
          <button class="back" @click="$emit('go-home')">‹ 返回首页</button>
        </div>
      </div>
    </header>

    <!-- ===== 坛首 ===== -->
    <section class="banner">
      <div class="banner-in">
        <div class="banner-txt">
          <span class="badge">❖ 元初山 · 论道之所</span>
          <h1>论 道 坛</h1>
          <p>刀意可论，道心可辨。诸界神尊于此开坛立论，共议功法、妖情与山河。</p>
          <div class="banner-stats">
            <div><b>{{ stats.posts ?? '—' }}</b><span>论题</span></div>
            <div><b>{{ stats.replies ?? '—' }}</b><span>回帖</span></div>
            <div><b>{{ stats.likes ?? '—' }}</b><span>共鸣</span></div>
            <div><b>{{ stats.today ?? '—' }}</b><span>今日新帖</span></div>
          </div>
        </div>
        <button class="compose-big" @click="openCompose">
          <span class="cb-ico">✎</span>
          <span class="cb-txt">开 坛 立 论</span>
          <small>{{ isGuest ? '游客需先觉醒神尊根基' : '以道会友 · 立此为帖' }}</small>
        </button>
      </div>
    </section>

    <!-- ===== 主体 ===== -->
    <main class="layout">
      <!-- 左侧栏 -->
      <aside class="side">
        <div class="side-card">
          <div class="sc-head"><span>◈</span><h3>论道板块</h3></div>
          <button v-for="c in categories" :key="c.key"
                  class="side-cat" :class="{ on: category === c.key && !activePost }"
                  :style="{ '--c': c.color }" @click="pickCategory(c.key)">
            <i></i><span>{{ c.label }}</span>
            <em>{{ (stats.category_counts || {})[c.key] ?? 0 }}</em>
          </button>
        </div>

        <div class="side-card">
          <div class="sc-head"><span>✦</span><h3>坛规</h3></div>
          <ul class="rules">
            <li>一 · 以道会友，勿作人身攻讦</li>
            <li>二 · 勿泄根基口令与私隐</li>
            <li>三 · 勿造妖言、妄传妖情</li>
            <li>四 · 违者由管理员撤帖</li>
          </ul>
        </div>

        <div class="side-card">
          <div class="sc-head"><span>♨</span><h3>热议论题</h3></div>
          <button v-for="(h, i) in hotList" :key="h.id" class="hot-item" @click="openPost(h)">
            <b>{{ i + 1 }}</b><span>{{ h.title }}</span>
            <em>♡ {{ h.like_count || 0 }}</em>
          </button>
          <p v-if="!hotList.length" class="hot-empty">暂无热议</p>
        </div>
      </aside>

      <!-- 内容区 -->
      <div class="content">
        <!-- 列表视图 -->
        <template v-if="!activePost">
          <div class="toolbar">
            <div class="tabs">
              <button v-for="c in categories" :key="c.key"
                      :class="{ on: category === c.key }" @click="pickCategory(c.key)">{{ c.label }}</button>
            </div>
            <div class="tools">
              <div class="search">
                <span>⌕</span>
                <input v-model="keyword" type="text" maxlength="40" placeholder="检索论题 / 道友…"
                       @keyup.enter="doSearch" />
                <button @click="doSearch">检索</button>
              </div>
              <select v-model="sort" class="sort" @change="reload">
                <option value="latest">最新论道</option>
                <option value="hot">最热论道</option>
              </select>
              <button class="refresh" :disabled="loading" @click="reload">↻</button>
            </div>
          </div>

          <div class="list-head">
            <span>共 <b>{{ total }}</b> 篇论题 · 第 {{ page }} / {{ Math.max(1, pages) }} 页</span>
            <span class="lh-tip">{{ sort === 'hot' ? '按共鸣与回帖排序' : '按最后回帖时间排序' }}</span>
          </div>

          <div v-if="loading && !posts.length" class="state"><div class="state-mark">◇</div><p>正在翻阅论道卷宗…</p></div>
          <div v-else-if="!posts.length" class="state">
            <div class="state-mark">◇</div>
            <p>此间尚无论题，何不开坛立论？</p>
            <button class="state-btn" @click="openCompose">开 坛 立 论</button>
          </div>

          <article v-for="p in posts" :key="p.id" class="post-card"
                   :style="{ '--c': colorOf(p.category) }" @click="openPost(p)">
            <div class="pc-bar"></div>
            <div class="pc-main">
              <div class="pc-top">
                <span class="pin" v-if="p.is_pinned">置顶</span>
                <span class="cat">{{ p.category }}</span>
                <h3>{{ p.title }}</h3>
              </div>
              <p class="pc-excerpt">{{ p.content }}</p>
              <div class="pc-meta">
                <span class="author"><i>{{ (p.author_alias || p.author_name || '道').slice(0, 1) }}</i>{{ p.author_alias || p.author_name }}</span>
                <span>{{ fmt(p.created_at) }}</span>
                <span class="dot">·</span>
                <span>阅 {{ p.view_count || 0 }}</span>
                <span>♡ {{ p.like_count || 0 }}</span>
                <span>✉ {{ p.reply_count || 0 }}</span>
                <span class="pc-go">入帖 ›</span>
              </div>
            </div>
          </article>

          <div class="pager" v-if="pages > 1">
            <button :disabled="page <= 1" @click="goPage(page - 1)">‹ 上一页</button>
            <span>{{ page }} / {{ pages }}</span>
            <button :disabled="page >= pages" @click="goPage(page + 1)">下一页 ›</button>
          </div>
        </template>

        <!-- 详情视图 -->
        <template v-else>
          <div class="detail-top">
            <button class="detail-back" @click="backToList">‹ 返回论题列表</button>
            <span class="detail-crumb">{{ activePost.category }} / 第 {{ activePost.id }} 帖</span>
          </div>

          <article class="detail-card" :style="{ '--c': colorOf(activePost.category) }">
            <div class="dc-head">
              <span class="cat">{{ activePost.category }}</span>
              <span class="pin" v-if="activePost.is_pinned">置顶</span>
              <h2>{{ activePost.title }}</h2>
            </div>
            <div class="dc-meta">
              <span class="author"><i>{{ (activePost.author_alias || activePost.author_name || '道').slice(0, 1) }}</i>{{ activePost.author_alias || activePost.author_name }}</span>
              <span>立于 {{ fmt(activePost.created_at) }}</span>
              <span>阅卷 {{ activePost.view_count || 0 }}</span>
              <span>回帖 {{ activePost.reply_count || 0 }}</span>
            </div>
            <div class="dc-body">{{ activePost.content }}</div>
            <div class="dc-ops">
              <button class="like" :class="{ on: activePost.liked }" :disabled="busy" @click="doLike">
                {{ activePost.liked ? '♥ 已共鸣' : '♡ 共 鸣' }} <b>{{ activePost.like_count || 0 }}</b>
              </button>
              <button class="reply-jump" @click="focusReply">✉ 回帖 {{ replies.length }}</button>
              <button class="del" v-if="canDelete(activePost)" :disabled="busy" @click="doDelete">撤下论题</button>
              <span class="dc-tip" v-else-if="isGuest">游客仅可阅卷，觉醒后方可参与论道</span>
            </div>
          </article>

          <section class="reply-zone">
            <div class="rz-head"><span>❖</span><h3>诸道友回帖</h3><em>共 {{ replies.length }} 楼</em></div>
            <div v-if="!replies.length" class="state small"><p>尚无回帖，抢先论道一二？</p></div>
            <div v-for="r in replies" :key="r.id" class="reply-card">
              <div class="rc-floor">{{ r.floor_no }}<small>楼</small></div>
              <div class="rc-body">
                <div class="rc-meta">
                  <b>{{ r.author_alias || r.author_name }}</b>
                  <span>{{ fmt(r.created_at) }}</span>
                </div>
                <p>{{ r.content }}</p>
              </div>
            </div>

            <div class="reply-form" ref="replyBox">
              <textarea v-model="replyText" maxlength="2000" rows="3"
                        :placeholder="isGuest ? '游客仅可阅卷，请先觉醒神尊根基…' : '写下你的道法见解…'"
                        :disabled="isGuest"></textarea>
              <div class="rf-foot">
                <span>{{ replyText.length }} / 2000</span>
                <button :disabled="busy || isGuest || !replyText.trim()" @click="doReply">发 布 回 帖</button>
              </div>
            </div>
          </section>
        </template>
      </div>
    </main>

    <!-- ===== 发帖弹窗 ===== -->
    <transition name="fade">
      <div class="modal" v-if="compose" @click.self="compose = false">
        <div class="modal-card">
          <button class="modal-x" @click="compose = false">✕</button>
          <span class="modal-tag">✎ 开 坛 立 论</span>
          <h3>立一道论题，会诸界道友</h3>
          <label class="fld"><span>论道板块</span>
            <select v-model="form.category">
              <option v-for="c in categories.slice(1)" :key="c.key" :value="c.key">{{ c.label }}</option>
            </select>
          </label>
          <label class="fld"><span>论题 <em>2–120 字</em></span>
            <input v-model="form.title" type="text" maxlength="120" placeholder="如：元神淬体诀第七重的关窍…" />
          </label>
          <label class="fld"><span>正文 <em>2–2000 字</em></span>
            <textarea v-model="form.content" maxlength="2000" rows="6" placeholder="详述你的见解、疑问或见闻…"></textarea>
          </label>
          <div class="modal-foot">
            <span>{{ form.content.length }} / 2000</span>
            <button class="cancel" @click="compose = false">收 卷</button>
            <button class="submit" :disabled="busy" @click="doCompose">{{ busy ? '立论中…' : '立 帖' }}</button>
          </div>
        </div>
      </div>
    </transition>

    <div class="toast" :class="{ show: toastShow, err: toastErr }">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  fetchForumPosts, fetchForumPost, fetchForumStats, createForumPost,
  createForumReply, toggleForumLike, deleteForumPost
} from '../api'

const props = defineProps({ avatar: { type: Object, default: null } })
const emit = defineEmits(['go-home'])

/* ===== 氛围粒子 ===== */
const palette = ['#2dd4bf', '#e9c46a', '#ef4454', '#a78bfa', '#ffffff']
const embers = Array.from({ length: 20 }, () => {
  const c = palette[Math.floor(Math.random() * palette.length)]
  const size = 2 + Math.random() * 3
  return {
    left: (Math.random() * 100).toFixed(2) + '%', background: c, boxShadow: `0 0 8px ${c}`,
    width: size + 'px', height: size + 'px',
    animation: `rise linear infinite ${(8 + Math.random() * 8).toFixed(2)}s ${(-Math.random() * 10).toFixed(2)}s`
  }
})

/* ===== 身份 ===== */
const isGuest = computed(() => !props.avatar || !!props.avatar.isGuest)
const isAdmin = computed(() => !isGuest.value && Number(props.avatar.permission) === 0)
const displayName = computed(() => {
  const a = props.avatar || {}
  return a.isGuest ? (a.deity_name || '游客') : (a.myth_name || a.deity_name || '道友')
})
const cred = computed(() => isGuest.value ? null : { name: props.avatar.deity_name, spell: props.avatar.spell })
const viewer = computed(() => cred.value?.name || '')

function requireLogin() {
  if (isGuest.value) { toast('游客无神尊根基，请先觉醒后再参与论道', true); return false }
  return true
}

/* ===== 板块 ===== */
const categories = [
  { key: '全部', label: '全部', color: '#5eead4' },
  { key: '论道', label: '论道', color: '#e9c46a' },
  { key: '切磋', label: '切磋', color: '#2dd4bf' },
  { key: '妖情', label: '妖情', color: '#ef4454' },
  { key: '闲谈', label: '闲谈', color: '#a78bfa' }
]
function colorOf(c) { return (categories.find(x => x.key === c) || categories[0]).color }

/* ===== 列表状态 ===== */
const category = ref('全部')
const keyword = ref('')
const sort = ref('latest')
const page = ref(1)
const pageSize = 8
const posts = ref([])
const total = ref(0)
const pages = ref(1)
const stats = ref({})
const loading = ref(false)
const busy = ref(false)
const hotList = computed(() => stats.value.hot || [])

async function load() {
  loading.value = true
  const d = await fetchForumPosts({
    category: category.value === '全部' ? '' : category.value,
    keyword: keyword.value.trim(), sort: sort.value,
    page: page.value, pageSize, viewer: viewer.value
  })
  loading.value = false
  if (d.ok) {
    posts.value = Array.isArray(d.items) ? d.items : []
    total.value = Number(d.total) || 0
    pages.value = Number(d.pages) || 1
    page.value = Number(d.page) || 1
  } else {
    toast(d.msg || '论道卷宗读取失败', true)
    posts.value = []
  }
}
async function loadStats() {
  const d = await fetchForumStats()
  if (d.ok) stats.value = d
}
function reload() { load(); loadStats() }
function doSearch() { page.value = 1; load() }
function goPage(p) { page.value = Math.max(1, Math.min(pages.value, p)); load(); scrollTop() }
function pickCategory(k) { category.value = k; page.value = 1; activePost.value = null; load(); scrollTop() }
function scrollTop() { if (root.value) root.value.scrollTo({ top: 0, behavior: 'smooth' }) }

/* ===== 详情 ===== */
const activePost = ref(null)
const replies = ref([])
const replyText = ref('')
const replyBox = ref(null)

async function openPost(p) {
  const d = await fetchForumPost(p.id, viewer.value)
  if (!d.ok) { toast(d.msg || '论题打开失败', true); return }
  activePost.value = d.post
  replies.value = Array.isArray(d.replies) ? d.replies : []
  replyText.value = ''
  scrollTop()
}
function backToList() { activePost.value = null; reload() }
function focusReply() { if (replyBox.value) replyBox.value.scrollIntoView({ behavior: 'smooth', block: 'center' }) }
function canDelete(p) {
  if (isGuest.value) return false
  return isAdmin.value || p.author_name === props.avatar.deity_name
}

async function doLike() {
  if (!requireLogin() || !activePost.value) return
  busy.value = true
  const d = await toggleForumLike(cred.value, activePost.value.id)
  busy.value = false
  if (!d.ok) { toast(d.msg || '共鸣失败', true); return }
  activePost.value.liked = !!d.liked
  activePost.value.like_count = Math.max(0, (activePost.value.like_count || 0) + (d.liked ? 1 : -1))
  const hit = posts.value.find(x => x.id === activePost.value.id)
  if (hit) { hit.liked = !!d.liked; hit.like_count = activePost.value.like_count }
  toast(d.liked ? '已与此论题共鸣' : '已收回共鸣')
}

async function doReply() {
  if (!requireLogin() || !activePost.value) return
  const text = replyText.value.trim()
  if (text.length < 2) { toast('回帖至少 2 字', true); return }
  busy.value = true
  const d = await createForumReply(cred.value, activePost.value.id, text)
  busy.value = false
  if (!d.ok) { toast(d.msg || '回帖失败', true); return }
  replies.value.push(d.reply)
  activePost.value.reply_count = (activePost.value.reply_count || 0) + 1
  replyText.value = ''
  toast('回帖已立于坛上')
  loadStats()
}

async function doDelete() {
  if (!activePost.value) return
  if (!window.confirm('确定撤下此论题？撤下后将不在列表中显示（数据仍保留）。')) return
  busy.value = true
  const d = await deleteForumPost(cred.value, activePost.value.id)
  busy.value = false
  if (!d.ok) { toast(d.msg || '撤帖失败', true); return }
  toast('论题已撤下')
  backToList()
}

/* ===== 发帖 ===== */
const compose = ref(false)
const form = ref({ category: '论道', title: '', content: '' })
function openCompose() {
  if (!requireLogin()) return
  form.value = { category: category.value === '全部' ? '论道' : category.value, title: '', content: '' }
  compose.value = true
}
async function doCompose() {
  if (!requireLogin()) return
  const t = form.value.title.trim(), c = form.value.content.trim()
  if (t.length < 2) { toast('论题至少 2 字', true); return }
  if (c.length < 2) { toast('正文至少 2 字', true); return }
  busy.value = true
  const d = await createForumPost(cred.value, { category: form.value.category, title: t, content: c })
  busy.value = false
  if (!d.ok) { toast(d.msg || '立帖失败', true); return }
  compose.value = false
  keyword.value = ''
  page.value = 1
  activePost.value = null
  category.value = form.value.category
  reload()
  toast('论题已立于论道坛')
}

/* ===== 工具 ===== */
function fmt(t) {
  if (!t) return '—'
  return String(t).replace('T', ' ').slice(0, 16)
}
const toastMsg = ref(''), toastShow = ref(false), toastErr = ref(false)
let toastTimer = null
function toast(msg, err = false) {
  toastMsg.value = msg; toastErr.value = err; toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2400)
}

onMounted(reload)
</script>

<style scoped>
.forum{position:fixed;inset:0;overflow-y:auto;overflow-x:hidden;
  background:radial-gradient(120% 80% at 80% 0%,rgba(45,212,191,.10),transparent 55%),
    radial-gradient(100% 90% at 10% 100%,rgba(233,196,106,.08),transparent 55%),
    linear-gradient(180deg,var(--ink2),var(--ink) 70%);color:var(--text)}
.embers{position:fixed;inset:0;pointer-events:none;z-index:0;overflow:hidden}
.embers i{position:absolute;bottom:-10px;border-radius:50%;opacity:0}
@keyframes rise{0%{transform:translateY(0) scale(1);opacity:0}10%{opacity:.9}90%{opacity:.5}100%{transform:translateY(-105vh) scale(.4);opacity:0}}

/* 顶栏 */
.nav{position:sticky;top:0;z-index:30;background:linear-gradient(180deg,rgba(7,11,22,.94),rgba(7,11,22,.66));
  backdrop-filter:blur(14px);border-bottom:1px solid var(--line);box-shadow:0 8px 30px rgba(0,0,0,.4)}
.nav-inner{max-width:1280px;margin:0 auto;padding:13px 26px;display:flex;align-items:center;gap:22px}
.brand{display:flex;align-items:center;gap:11px;cursor:pointer;flex-shrink:0}
.seal{width:42px;height:42px;border-radius:11px;display:grid;place-items:center;font-weight:900;font-size:20px;color:#fff;
  background:linear-gradient(135deg,var(--gold),#b45309);box-shadow:0 0 22px rgba(233,196,106,.5)}
.brand-txt b{display:block;font-size:18px;letter-spacing:4px;font-weight:900}
.brand-txt span{font-size:10px;color:var(--muted);letter-spacing:.5px}
.crumbs{display:flex;align-items:center;gap:9px;font-size:13px;color:var(--muted);flex:1}
.crumbs a{cursor:pointer;transition:.15s}.crumbs a:hover{color:var(--jade)}
.crumbs .sep{opacity:.5}.crumbs .cur{color:var(--text)}
.nav-right{display:flex;align-items:center;gap:12px;flex-shrink:0}
.who{display:flex;align-items:center;gap:7px;font-size:13px;letter-spacing:1px;color:var(--text)}
.who i{width:28px;height:28px;border-radius:50%;display:grid;place-items:center;font-style:normal;font-weight:900;
  color:#04130f;background:linear-gradient(135deg,var(--gold),#f7f3e8)}
.who em{font-style:normal;font-size:11px;padding:2px 8px;border-radius:999px;color:var(--muted);
  background:rgba(148,163,184,.16);border:1px solid var(--line)}
.who em.gold{color:var(--gold);border-color:rgba(233,196,106,.45);background:rgba(233,196,106,.12)}
.back{padding:7px 14px;border:1px solid var(--line);border-radius:999px;background:none;color:var(--muted);
  cursor:pointer;font-family:inherit;font-size:12px;letter-spacing:1px;transition:.15s}
.back:hover{color:var(--jade);border-color:var(--jade)}

/* 坛首 */
.banner{position:relative;z-index:2;max-width:1280px;margin:0 auto;padding:36px 26px 8px}
.banner-in{display:flex;align-items:center;justify-content:space-between;gap:26px;
  padding:30px 34px;border-radius:22px;overflow:hidden;position:relative;
  background:linear-gradient(130deg,rgba(14,22,42,.86),rgba(7,11,22,.62));
  border:1px solid rgba(233,196,106,.28);box-shadow:0 22px 60px rgba(0,0,0,.45),inset 0 1px 0 rgba(255,255,255,.06)}
.banner-in::after{content:"";position:absolute;right:-70px;top:-90px;width:280px;height:280px;border-radius:50%;
  background:radial-gradient(circle,rgba(233,196,106,.22),transparent 70%);pointer-events:none}
.badge{display:inline-block;font-size:11.5px;letter-spacing:3px;color:var(--gold);padding:5px 14px;
  border:1px solid rgba(233,196,106,.4);border-radius:999px;background:rgba(233,196,106,.08)}
.banner-txt h1{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:46px;letter-spacing:12px;margin:14px 0 8px;
  background:linear-gradient(90deg,var(--gold),#fff,var(--jade));background-size:200% auto;
  -webkit-background-clip:text;background-clip:text;color:transparent;filter:drop-shadow(0 0 22px rgba(233,196,106,.35))}
.banner-txt p{font-size:13.5px;color:#c7d4e8;line-height:2;letter-spacing:1px}
.banner-stats{display:flex;gap:30px;margin-top:20px}
.banner-stats div{display:flex;flex-direction:column;gap:2px}
.banner-stats b{font-size:24px;color:var(--jade);font-variant-numeric:tabular-nums}
.banner-stats span{font-size:11px;color:var(--muted);letter-spacing:2px}
.compose-big{flex-shrink:0;width:210px;padding:22px 16px;border-radius:18px;cursor:pointer;font-family:inherit;
  display:flex;flex-direction:column;align-items:center;gap:8px;position:relative;z-index:2;transition:.2s;
  color:#04130f;background:linear-gradient(135deg,var(--gold),#f3d99a);
  border:1px solid rgba(255,255,255,.35);box-shadow:0 14px 36px rgba(233,196,106,.3)}
.compose-big:hover{transform:translateY(-4px);box-shadow:0 20px 46px rgba(233,196,106,.48)}
.cb-ico{font-size:26px}.cb-txt{font-size:17px;font-weight:900;letter-spacing:5px}
.compose-big small{font-size:11px;opacity:.72;letter-spacing:.5px}

/* 布局 */
.layout{position:relative;z-index:2;max-width:1280px;margin:0 auto;padding:24px 26px 70px;
  display:grid;grid-template-columns:238px 1fr;gap:22px;align-items:start}
.side{display:flex;flex-direction:column;gap:16px;position:sticky;top:88px}
.side-card{padding:18px 16px;border-radius:16px;background:var(--glass);
  border:1px solid rgba(255,255,255,.10);backdrop-filter:blur(12px);box-shadow:0 16px 40px rgba(0,0,0,.35)}
.sc-head{display:flex;align-items:center;gap:9px;margin-bottom:14px;padding-bottom:11px;border-bottom:1px solid var(--line)}
.sc-head span{color:var(--gold);font-size:16px}
.sc-head h3{font-size:14px;letter-spacing:2px;font-weight:900}
.side-cat{width:100%;display:flex;align-items:center;gap:10px;padding:9px 10px;margin-bottom:4px;border:none;
  border-radius:10px;background:none;color:var(--muted);cursor:pointer;font-family:inherit;font-size:13px;
  letter-spacing:1px;text-align:left;transition:.15s}
.side-cat i{width:7px;height:7px;border-radius:50%;background:var(--c);box-shadow:0 0 9px var(--c)}
.side-cat span{flex:1}.side-cat em{font-style:normal;font-size:11px;color:var(--muted)}
.side-cat:hover{background:rgba(255,255,255,.05);color:var(--text)}
.side-cat.on{color:var(--c);background:color-mix(in srgb,var(--c) 12%,transparent)}
.rules{list-style:none;display:flex;flex-direction:column;gap:9px}
.rules li{font-size:11.5px;color:var(--muted);line-height:1.6;letter-spacing:.5px;padding-left:12px;position:relative}
.rules li::before{content:"·";position:absolute;left:2px;color:var(--gold)}
.hot-item{width:100%;display:flex;align-items:center;gap:9px;padding:8px 4px;border:none;border-bottom:1px dashed rgba(255,255,255,.08);
  background:none;color:var(--text);cursor:pointer;font-family:inherit;text-align:left;transition:.15s}
.hot-item b{font-size:12px;color:var(--gold);width:16px;flex-shrink:0}
.hot-item span{flex:1;font-size:12px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.hot-item em{font-style:normal;font-size:11px;color:var(--muted);flex-shrink:0}
.hot-item:hover span{color:var(--jade)}
.hot-empty{font-size:12px;color:var(--muted);text-align:center;padding:12px}

/* 工具栏 */
.content{min-width:0}
.toolbar{display:flex;align-items:center;justify-content:space-between;gap:14px;flex-wrap:wrap;
  padding:12px 14px;border-radius:14px;background:var(--glass);border:1px solid rgba(255,255,255,.10)}
.tabs{display:flex;gap:5px;flex-wrap:wrap}
.tabs button{padding:7px 14px;border:1px solid transparent;border-radius:999px;background:none;color:var(--muted);
  cursor:pointer;font-family:inherit;font-size:12.5px;letter-spacing:1px;transition:.15s}
.tabs button:hover{color:var(--text)}
.tabs button.on{color:var(--jade);border-color:rgba(45,212,191,.45);background:rgba(45,212,191,.12)}
.tools{display:flex;align-items:center;gap:8px}
.search{display:flex;align-items:center;gap:6px;padding:5px 5px 5px 11px;border-radius:999px;
  background:rgba(7,11,22,.6);border:1px solid rgba(255,255,255,.12)}
.search span{color:var(--muted);font-size:15px}
.search input{width:140px;background:none;border:none;outline:none;color:var(--text);font-family:inherit;font-size:12.5px}
.search button{padding:5px 11px;border:none;border-radius:999px;cursor:pointer;font-family:inherit;font-size:11.5px;
  letter-spacing:1px;color:#04130f;background:linear-gradient(135deg,var(--jade),#5eead4)}
.sort,.refresh{padding:6px 10px;border-radius:9px;background:rgba(7,11,22,.6);color:var(--muted);
  border:1px solid rgba(255,255,255,.12);font-family:inherit;font-size:12px;cursor:pointer}
.sort option{background:#0b1224;color:var(--text)}
.refresh:hover{color:var(--jade);border-color:var(--jade)}
.list-head{display:flex;justify-content:space-between;align-items:center;margin:18px 4px 12px;font-size:12px;color:var(--muted);letter-spacing:1px}
.list-head b{color:var(--jade);font-size:15px}
.lh-tip{opacity:.75}

/* 帖子卡片 */
.post-card{position:relative;display:flex;margin-bottom:12px;border-radius:14px;overflow:hidden;cursor:pointer;
  background:var(--glass);border:1px solid rgba(255,255,255,.10);backdrop-filter:blur(10px);transition:.18s}
.post-card:hover{transform:translateY(-3px);border-color:color-mix(in srgb,var(--c) 55%,transparent);
  box-shadow:0 20px 44px rgba(0,0,0,.45),0 0 22px color-mix(in srgb,var(--c) 22%,transparent)}
.pc-bar{width:3px;flex-shrink:0;background:linear-gradient(180deg,var(--c),transparent)}
.pc-main{flex:1;min-width:0;padding:16px 18px 13px}
.pc-top{display:flex;align-items:center;gap:9px;min-width:0}
.pin{flex-shrink:0;font-size:10.5px;letter-spacing:1px;padding:3px 8px;border-radius:6px;color:#04130f;
  background:linear-gradient(135deg,var(--gold),#f7e6b0);font-weight:900}
.cat{flex-shrink:0;font-size:11px;letter-spacing:1px;padding:3px 9px;border-radius:999px;color:var(--c);
  border:1px solid color-mix(in srgb,var(--c) 45%,transparent);background:color-mix(in srgb,var(--c) 12%,transparent)}
.pc-top h3{font-size:16px;font-weight:900;letter-spacing:1px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.pc-excerpt{margin:9px 0 12px;font-size:12.5px;line-height:1.8;color:var(--muted);
  display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}
.pc-meta{display:flex;align-items:center;gap:12px;flex-wrap:wrap;font-size:11.5px;color:var(--muted)}
.author{display:inline-flex;align-items:center;gap:6px;color:#cbd8ec}
.author i{width:20px;height:20px;border-radius:50%;display:grid;place-items:center;font-style:normal;font-size:10px;
  font-weight:900;color:#04130f;background:linear-gradient(135deg,var(--gold),#f7f3e8)}
.dot{opacity:.5}.pc-go{margin-left:auto;color:var(--c);letter-spacing:2px;white-space:nowrap}
.post-card:hover .pc-go{letter-spacing:4px}

.state{text-align:center;padding:56px 0;color:var(--muted)}
.state.small{padding:24px 0;font-size:13px}
.state-mark{font-size:46px;color:var(--line);margin-bottom:12px}
.state p{letter-spacing:1px;font-size:13px}
.state-btn{margin-top:16px;padding:9px 22px;border:none;border-radius:999px;cursor:pointer;font-family:inherit;
  font-size:13px;letter-spacing:3px;color:#04130f;background:linear-gradient(135deg,var(--gold),#f7e6b0)}
.pager{display:flex;align-items:center;justify-content:center;gap:16px;margin-top:20px}
.pager button{padding:8px 16px;border:1px solid var(--line);border-radius:999px;background:none;color:var(--muted);
  cursor:pointer;font-family:inherit;font-size:12px;letter-spacing:1px}
.pager button:hover:not(:disabled){color:var(--jade);border-color:var(--jade)}
.pager button:disabled{opacity:.35;cursor:not-allowed}
.pager span{font-size:12px;color:var(--muted)}

/* 详情 */
.detail-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}
.detail-back{padding:8px 16px;border:1px solid var(--line);border-radius:999px;background:none;color:var(--muted);
  cursor:pointer;font-family:inherit;font-size:12px;letter-spacing:1px}
.detail-back:hover{color:var(--jade);border-color:var(--jade)}
.detail-crumb{font-size:12px;color:var(--muted);letter-spacing:1px}
.detail-card{padding:24px 26px;border-radius:18px;background:var(--glass);
  border:1px solid color-mix(in srgb,var(--c) 38%,transparent);box-shadow:0 20px 50px rgba(0,0,0,.4)}
.dc-head{display:flex;align-items:center;gap:10px;flex-wrap:wrap}
.dc-head h2{width:100%;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:27px;letter-spacing:2px;margin-top:8px;line-height:1.5}
.dc-meta{display:flex;align-items:center;gap:16px;flex-wrap:wrap;margin:14px 0 20px;padding-bottom:16px;
  border-bottom:1px solid var(--line);font-size:12px;color:var(--muted)}
.dc-body{font-size:14px;line-height:2.1;letter-spacing:.5px;color:#d7e2f2;white-space:pre-wrap;word-break:break-word}
.dc-ops{display:flex;align-items:center;gap:10px;flex-wrap:wrap;margin-top:24px;padding-top:18px;border-top:1px solid var(--line)}
.like,.reply-jump,.del{padding:9px 18px;border-radius:999px;cursor:pointer;font-family:inherit;font-size:12.5px;letter-spacing:1px;transition:.15s}
.like{border:1px solid rgba(239,68,84,.45);background:rgba(239,68,84,.10);color:var(--crimson)}
.like.on{border-color:var(--crimson);background:rgba(239,68,84,.24);box-shadow:0 0 18px rgba(239,68,84,.3)}
.like b{margin-left:6px}
.reply-jump{border:1px solid var(--line);background:none;color:var(--jade)}
.del{border:1px solid rgba(148,163,184,.35);background:none;color:var(--muted);margin-left:auto}
.del:hover{color:var(--crimson);border-color:var(--crimson)}
.dc-tip{margin-left:auto;font-size:11.5px;color:var(--muted)}

/* 回帖 */
.reply-zone{margin-top:22px}
.rz-head{display:flex;align-items:center;gap:10px;margin-bottom:14px}
.rz-head span{color:var(--gold);font-size:18px}
.rz-head h3{font-size:17px;letter-spacing:3px}
.rz-head em{font-style:normal;font-size:12px;color:var(--muted);margin-left:auto}
.reply-card{display:flex;gap:14px;margin-bottom:10px;padding:16px 18px;border-radius:14px;
  background:var(--glass);border:1px solid rgba(255,255,255,.09)}
.rc-floor{flex-shrink:0;width:46px;text-align:center;font-size:20px;font-weight:900;color:var(--gold);
  font-family:"ZCOOL XiaoWei",serif}
.rc-floor small{display:block;font-size:10px;color:var(--muted);font-weight:400;letter-spacing:1px}
.rc-body{flex:1;min-width:0}
.rc-meta{display:flex;align-items:center;gap:12px;margin-bottom:8px;font-size:12px;color:var(--muted)}
.rc-meta b{color:var(--text);font-size:13px}
.rc-body p{font-size:13px;line-height:1.95;color:#d0dcee;white-space:pre-wrap;word-break:break-word}
.reply-form{margin-top:16px;padding:16px;border-radius:14px;background:rgba(7,11,22,.55);border:1px solid var(--line)}
.reply-form textarea{width:100%;padding:12px 14px;border-radius:10px;resize:vertical;outline:none;
  background:rgba(7,11,22,.6);border:1px solid rgba(255,255,255,.12);color:var(--text);
  font-family:inherit;font-size:13px;line-height:1.8}
.reply-form textarea:focus{border-color:var(--jade)}
.rf-foot{display:flex;align-items:center;justify-content:flex-end;gap:14px;margin-top:10px}
.rf-foot span{font-size:11.5px;color:var(--muted)}
.rf-foot button{padding:9px 22px;border:none;border-radius:999px;cursor:pointer;font-family:inherit;
  font-size:12.5px;font-weight:700;letter-spacing:2px;color:#04130f;background:linear-gradient(135deg,var(--jade),#5eead4)}
.rf-foot button:disabled{opacity:.45;cursor:not-allowed}

/* 弹窗 */
.modal{position:fixed;inset:0;z-index:80;display:grid;place-items:center;padding:24px;
  background:rgba(4,7,14,.7);backdrop-filter:blur(6px)}
.modal-card{position:relative;width:100%;max-width:560px;padding:32px;border-radius:20px;
  background:linear-gradient(160deg,rgba(14,22,42,.98),rgba(7,11,22,.98));
  border:1px solid rgba(233,196,106,.4);box-shadow:0 30px 80px rgba(0,0,0,.6);animation:cardIn .35s ease both}
@keyframes cardIn{from{opacity:0;transform:translateY(16px) scale(.97)}to{opacity:1;transform:none}}
.modal-x{position:absolute;top:16px;right:18px;width:30px;height:30px;border-radius:50%;border:1px solid var(--line);
  background:none;color:var(--muted);cursor:pointer;font-size:13px}
.modal-x:hover{color:var(--crimson);border-color:var(--crimson)}
.modal-tag{font-size:11px;letter-spacing:3px;color:var(--gold)}
.modal-card h3{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:23px;letter-spacing:2px;margin:10px 0 22px}
.fld{display:block;margin-bottom:16px}
.fld>span{display:flex;justify-content:space-between;font-size:12px;color:var(--muted);letter-spacing:1px;margin-bottom:7px}
.fld em{font-style:normal;opacity:.7}
.fld input,.fld select,.fld textarea{width:100%;padding:11px 13px;border-radius:10px;outline:none;
  background:rgba(7,11,22,.6);border:1px solid rgba(255,255,255,.14);color:var(--text);
  font-family:inherit;font-size:13px;line-height:1.8;resize:vertical}
.fld input:focus,.fld select:focus,.fld textarea:focus{border-color:var(--jade)}
.fld select option{background:#0b1224}
.modal-foot{display:flex;align-items:center;gap:10px;margin-top:20px}
.modal-foot span{flex:1;font-size:11.5px;color:var(--muted)}
.cancel,.submit{padding:10px 20px;border-radius:10px;cursor:pointer;font-family:inherit;font-size:12.5px;letter-spacing:2px}
.cancel{border:1px solid var(--line);background:none;color:var(--muted)}
.submit{border:none;font-weight:700;color:#04130f;background:linear-gradient(135deg,var(--gold),#f7e6b0)}
.submit:disabled{opacity:.5;cursor:wait}
.fade-enter-active,.fade-leave-active{transition:opacity .25s}
.fade-enter-from,.fade-leave-to{opacity:0}

/* toast */
.toast{position:fixed;left:50%;top:82px;transform:translateX(-50%) translateY(-20px);z-index:90;
  padding:11px 22px;border-radius:12px;font-size:13px;font-weight:700;letter-spacing:1px;opacity:0;pointer-events:none;
  transition:.3s;color:#04130f;background:linear-gradient(135deg,var(--jade-deep),var(--jade));
  box-shadow:0 14px 40px rgba(45,212,191,.4)}
.toast.err{color:#fff;background:linear-gradient(135deg,#b91c1c,var(--crimson))}
.toast.show{opacity:1;transform:translateX(-50%) translateY(0)}

@media(max-width:1000px){.layout{grid-template-columns:1fr}.side{position:static;flex-direction:row;flex-wrap:wrap}
  .side-card{flex:1;min-width:220px}.banner-in{flex-direction:column;align-items:flex-start}.compose-big{width:100%}}
@media(max-width:640px){.crumbs{display:none}.banner-txt h1{font-size:34px;letter-spacing:6px}
  .banner-stats{gap:16px}.nav-inner{padding:12px 16px}.banner,.layout{padding-left:16px;padding-right:16px}}
</style>
