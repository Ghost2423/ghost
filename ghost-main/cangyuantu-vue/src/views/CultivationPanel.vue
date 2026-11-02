<template>
  <div class="cultivation" ref="root">
    <!-- 氛围：流光粒子 -->
    <div class="embers">
      <i v-for="(e, i) in embers" :key="i" :style="e"></i>
    </div>

    <!-- ===== 顶部栏 ===== -->
    <header class="nav">
      <div class="nav-inner">
        <div class="brand" @click="$emit('go-home')">
          <div class="seal">沧</div>
          <div class="brand-txt">
            <b>修炼殿</b>
            <span>XIU LIAN DIAN · 元界</span>
          </div>
        </div>
        <nav class="crumbs">
          <a @click="$emit('go-home')">首页</a>
          <span class="sep">›</span>
          <span class="cur">修炼殿</span>
        </nav>
        <div class="nav-right" v-if="cul">
          <span class="pill power">⚡ 神尊力 {{ cul.power }}</span>
          <span class="pill copper">◉ 铜币 {{ cul.copper }}</span>
          <button class="back" @click="$emit('go-home')">‹ 返回首页</button>
        </div>
      </div>
    </header>

    <!-- ===== 修炼状态主卡 ===== -->
    <section class="hero" v-if="cul">
      <div class="hero-card">
        <div class="who">
          <span class="who-name">{{ cul.entry_name }}</span>
          <span class="who-account">{{ cul.name }}</span>
        </div>

        <div class="realm-zone">
          <span class="stage-tag">{{ cul.stage }}</span>
          <h1 class="realm-name">{{ cul.realm }}</h1>
          <span class="title-badge" v-if="cul.title">{{ cul.title }}</span>
          <span class="apex-badge" v-if="cul.apex">沧元之巅</span>
        </div>

        <!-- 突破进度 -->
        <div class="progress" v-if="!cul.apex">
          <div class="progress-info">
            <span>突破「{{ cul.next_realm }}」还需神尊力 {{ Math.max(0, cul.break_cost - cul.power) }}</span>
            <span>{{ cul.power }} / {{ cul.break_cost }}</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progressPct + '%' }"></div>
          </div>
        </div>
        <div class="progress apex" v-else>
          <p>永恒之境，沧元界再无更高境界。天地为证，岁月为名。</p>
        </div>

        <!-- 行动区 -->
        <div class="actions">
          <!-- 突破 -->
          <div class="action-card">
            <div class="ac-head">
              <span class="ac-ico">✦</span>
              <h3>境界突破</h3>
            </div>
            <p class="ac-desc" v-if="!cul.apex">
              自「{{ cul.realm }}」冲击「{{ cul.next_realm }}」，需 <b>{{ cul.break_cost }}</b> 神尊力。
            </p>
            <p class="ac-desc" v-else>已至永恒，无可突破。</p>
            <button class="ac-btn jade" :disabled="busy || cul.apex || cul.power < cul.break_cost"
                    @click="doBreakthrough">
              {{ cul.power >= cul.break_cost ? '冲 击 境 界' : '神尊力不足' }}
            </button>
          </div>

          <!-- 秘境 -->
          <div class="action-card">
            <div class="ac-head">
              <span class="ac-ico">❖</span>
              <h3>秘境修炼</h3>
            </div>
            <p class="ac-desc">
              耗 <b>{{ cul.secret.cost }}</b> 铜币入秘境，一次得 <b>{{ cul.secret.gain }}</b> 神尊力。
              今日余 <b>{{ cul.secret.left }}</b> / {{ cul.secret.limit }} 次。
            </p>
            <div class="ac-meter"><i :style="{ width: (cul.secret.used / cul.secret.limit * 100) + '%' }"></i></div>
            <button class="ac-btn teal" :disabled="busy || cul.secret.left <= 0 || cul.copper < cul.secret.cost"
                    @click="doSecret">
              {{ cul.secret.left <= 0 ? '今日次数已尽' : (cul.copper < cul.secret.cost ? '铜币不足' : '入 秘 境 修 炼') }}
            </button>
          </div>

          <!-- 切磋 -->
          <div class="action-card">
            <div class="ac-head">
              <span class="ac-ico">⚔</span>
              <h3>神尊切磋</h3>
            </div>
            <p class="ac-desc">
              与同道切磋，双方各得 <b>{{ cul.spar.gain_power }}</b> 神尊力 + <b>{{ cul.spar.gain_copper }}</b> 铜币。
            </p>
            <div class="spar-form">
              <input v-model="sparTarget" type="text" maxlength="64"
                     placeholder="对方神尊相名号…" @keyup.enter="doSpar" />
              <button class="ac-btn gold" :disabled="busy || !sparTarget.trim()" @click="doSpar">切 磋</button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 加载/异常态 -->
    <section class="loading" v-else>
      <div class="loading-mark">◇</div>
      <p>{{ loadMsg }}</p>
      <button class="retry" @click="load" v-if="loadFailed">重新探查</button>
    </section>

    <!-- ===== 境界阶梯 + 功法阁 ===== -->
    <main class="main" v-if="cul">
      <!-- 境界阶梯 -->
      <section class="block">
        <div class="block-head">
          <span class="bh-icon">☰</span>
          <h2>境界阶梯</h2>
          <span class="bh-line"></span>
          <span class="bh-tip">十九境通天，一境一重天</span>
        </div>
        <div class="ladder">
          <div class="stage-group" v-for="g in ladderGroups" :key="g.stage">
            <div class="sg-title">{{ g.stage }}</div>
            <div class="sg-realms">
              <div v-for="r in g.realms" :key="r.name" class="realm-chip"
                   :class="{ now: r.index === cul.realm_index, past: r.index < cul.realm_index }">
                <span class="rc-idx">{{ r.index + 1 }}</span>
                <span class="rc-name">{{ r.name }}</span>
                <span class="rc-title" v-if="r.title">{{ r.title }}</span>
                <span class="rc-mark" v-if="r.index < cul.realm_index">✓</span>
                <span class="rc-mark now-mark" v-else-if="r.index === cul.realm_index">今</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 功法阁 -->
      <section class="block">
        <div class="block-head">
          <span class="bh-icon">✧</span>
          <h2>功法阁</h2>
          <span class="bh-line"></span>
          <span class="bh-tip">以铜币求得功法，参悟即得神尊力</span>
        </div>
        <div class="gongfa-grid">
          <article v-for="g in cul.gongfa_shop" :key="g.name" class="gongfa-card"
                   :class="{ owned: g.owned }">
            <div class="gf-top">
              <span class="gf-tag">{{ g.tag }}</span>
              <span class="gf-owned" v-if="g.owned">已修得</span>
            </div>
            <h3 class="gf-name">{{ g.name }}</h3>
            <p class="gf-desc">{{ g.desc }}</p>
            <div class="gf-foot">
              <div class="gf-meta">
                <span class="gf-price">◉ {{ g.price }} 铜币</span>
                <span class="gf-gain">⚡ +{{ g.power_gain }} 神尊力</span>
              </div>
              <button class="gf-buy" v-if="!g.owned"
                      :disabled="busy || cul.copper < g.price" @click="doBuy(g)">
                {{ cul.copper < g.price ? '铜币不足' : '求 得' }}
              </button>
              <span class="gf-done" v-else>✓ 收录</span>
            </div>
          </article>
        </div>
      </section>
    </main>

    <footer class="foot">沧元图 · 修炼系统 —— 秘境淬体 · 功法参悟 · 切磋问道 · 境界通天</footer>

    <!-- 突破特效 -->
    <transition name="fade">
      <div class="break-flash" v-if="breakFx">
        <div class="bf-card">
          <div class="bf-glow"></div>
          <p class="bf-from">{{ breakFx.from }}</p>
          <span class="bf-arrow">↓</span>
          <p class="bf-to">{{ breakFx.to }}</p>
          <p class="bf-title" v-if="breakFx.title">晋「{{ breakFx.title }}」</p>
        </div>
      </div>
    </transition>

    <!-- 提示 toast -->
    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchCultivation, secretTraining, sparWith, buyGongfa, breakthroughRealm } from '../api'

const props = defineProps({ avatar: { type: Object, default: null } })
defineEmits(['go-home'])

const root = ref(null)

/* ===== 氛围粒子（与首页同款） ===== */
const palette = ['#2dd4bf', '#e9c46a', '#ef4454', '#5eead4', '#ffffff']
const embers = Array.from({ length: 20 }, () => {
  const c = palette[Math.floor(Math.random() * palette.length)]
  const size = 2 + Math.random() * 3
  return {
    left: (Math.random() * 100).toFixed(2) + '%',
    background: c,
    boxShadow: `0 0 8px ${c}`,
    width: size + 'px',
    height: size + 'px',
    animation: `rise linear infinite ${(7 + Math.random() * 8).toFixed(2)}s ${(-Math.random() * 10).toFixed(2)}s`
  }
})

/* ===== 数据 ===== */
const cul = ref(null)
const busy = ref(false)
const loadFailed = ref(false)
const loadMsg = ref('正在探查根基…')
const sparTarget = ref('')
const breakFx = ref(null)

function cred() {
  const a = props.avatar || {}
  return { name: a.deity_name, spell: a.spell }
}

async function load() {
  const a = props.avatar || {}
  if (!a.deity_name) { loadMsg.value = '游客无修炼根基，请先觉醒神尊'; loadFailed.value = true; return }
  loadFailed.value = false
  loadMsg.value = '正在探查根基…'
  cul.value = null
  const d = await fetchCultivation(a.deity_name)
  if (d && d.ok && d.cultivation) {
    cul.value = d.cultivation
  } else {
    loadFailed.value = true
    loadMsg.value = (d && d.msg) || '无法连接元界（后端未启动？）'
  }
}
onMounted(load)

/* ===== 计算 ===== */
const progressPct = computed(() => {
  if (!cul.value || cul.value.apex || !cul.value.break_cost) return 0
  return Math.min(100, Math.round(cul.value.power / cul.value.break_cost * 100))
})

const ladderGroups = computed(() => {
  if (!cul.value || !Array.isArray(cul.value.ladder)) return []
  const groups = []
  for (const r of cul.value.ladder) {
    const last = groups[groups.length - 1]
    if (last && last.stage === r.stage) last.realms.push(r)
    else groups.push({ stage: r.stage, realms: [r] })
  }
  return groups
})

/* ===== 行动 ===== */
async function doSecret() {
  if (busy.value) return
  busy.value = true
  const d = await secretTraining(cred())
  busy.value = false
  if (d.ok) {
    cul.value = d.cultivation
    toast(`秘境修炼一番，得 ${d.gain_power} 神尊力（今日余 ${d.left} 次）`)
  } else toast(d.msg || '入秘境失败')
}

async function doSpar() {
  const t = sparTarget.value.trim()
  if (!t || busy.value) return
  busy.value = true
  const d = await sparWith(cred(), t)
  busy.value = false
  if (d.ok) {
    cul.value = d.cultivation
    sparTarget.value = ''
    toast(`与「${d.target}」切磋完毕，各得 ${d.gain_power} 神尊力 + ${d.gain_copper} 铜币`)
  } else toast(d.msg || '切磋失败')
}

async function doBuy(g) {
  if (busy.value) return
  busy.value = true
  const d = await buyGongfa(cred(), g.name)
  busy.value = false
  if (d.ok) {
    cul.value = d.cultivation
    toast(`参悟「${d.gongfa}」有成，得 ${d.gain_power} 神尊力`)
  } else toast(d.msg || '求取功法失败')
}

async function doBreakthrough() {
  if (busy.value) return
  busy.value = true
  const d = await breakthroughRealm(cred())
  busy.value = false
  if (d.ok) {
    cul.value = d.cultivation
    breakFx.value = d
    setTimeout(() => { breakFx.value = null }, 2600)
    toast(`突破成功！已入「${d.to}」境`)
  } else toast(d.msg || '突破失败')
}

/* ===== toast ===== */
const toastMsg = ref('')
const toastShow = ref(false)
let toastTimer = null
function toast(msg) {
  toastMsg.value = msg
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2600)
}
</script>

<style scoped>
.cultivation{position:fixed;inset:0;overflow-y:auto;overflow-x:hidden;
  background:
    radial-gradient(120% 80% at 80% 0%,rgba(45,212,191,.10),transparent 55%),
    radial-gradient(100% 90% at 10% 100%,rgba(239,68,84,.10),transparent 55%),
    linear-gradient(180deg,var(--ink2),var(--ink) 70%);
  color:var(--text)}
.embers{position:fixed;inset:0;pointer-events:none;z-index:0;overflow:hidden}
.embers i{position:absolute;bottom:-10px;border-radius:50%;opacity:0}
@keyframes rise{
  0%{transform:translateY(0) scale(1);opacity:0}
  10%{opacity:.9}90%{opacity:.5}
  100%{transform:translateY(-105vh) scale(.4);opacity:0}}

/* ===== 顶部栏 ===== */
.nav{position:sticky;top:0;z-index:30;
  background:linear-gradient(180deg,rgba(7,11,22,.92),rgba(7,11,22,.62));
  backdrop-filter:blur(14px);border-bottom:1px solid var(--line);
  box-shadow:0 8px 30px rgba(0,0,0,.4)}
.nav-inner{max-width:1240px;margin:0 auto;display:flex;align-items:center;gap:26px;padding:14px 26px}
.brand{display:flex;align-items:center;gap:12px;cursor:pointer;flex-shrink:0}
.seal{width:42px;height:42px;border-radius:11px;
  background:linear-gradient(135deg,var(--crimson),#b91c1c);
  display:grid;place-items:center;font-weight:900;color:#fff;font-size:20px;
  box-shadow:0 0 22px rgba(239,68,84,.55)}
.brand-txt b{font-size:18px;letter-spacing:4px;font-weight:900;display:block}
.brand-txt span{font-size:11px;color:var(--muted);letter-spacing:2px}
.crumbs{display:flex;align-items:center;gap:10px;flex:1;font-size:13px;letter-spacing:1px}
.crumbs a{color:var(--muted);cursor:pointer;transition:.15s}
.crumbs a:hover{color:var(--jade)}
.crumbs .sep{color:var(--line)}
.crumbs .cur{color:var(--jade)}
.nav-right{display:flex;align-items:center;gap:10px;flex-shrink:0}
.pill{padding:6px 14px;border-radius:999px;font-size:13px;letter-spacing:1px;font-weight:700;
  border:1px solid var(--line)}
.pill.power{color:var(--jade);background:rgba(45,212,191,.10)}
.pill.copper{color:var(--gold);background:rgba(233,196,106,.10);border-color:rgba(233,196,106,.4)}
.back{padding:6px 14px;border:1px solid var(--line);border-radius:999px;background:none;
  color:var(--muted);cursor:pointer;font-family:inherit;font-size:12px;letter-spacing:2px;transition:.15s}
.back:hover{color:var(--jade);border-color:var(--jade)}

/* ===== 修炼状态主卡 ===== */
.hero{max-width:1240px;margin:34px auto 0;padding:0 26px;position:relative;z-index:2}
.hero-card{padding:36px 38px;border-radius:24px;position:relative;overflow:hidden;
  background:linear-gradient(160deg,rgba(14,22,42,.78),rgba(7,11,22,.6));
  border:1px solid rgba(255,255,255,.12);backdrop-filter:blur(12px);
  box-shadow:0 22px 60px rgba(0,0,0,.45),inset 0 1px 0 rgba(255,255,255,.07)}
.hero-card::before{content:"";position:absolute;inset:-1px;border-radius:25px;padding:1.5px;
  background:linear-gradient(130deg,var(--jade),transparent 42%,var(--gold));
  -webkit-mask:linear-gradient(#000 0 0) content-box,linear-gradient(#000 0 0);
  -webkit-mask-composite:xor;mask-composite:exclude;pointer-events:none;opacity:.55}

.who{display:flex;align-items:baseline;gap:14px;flex-wrap:wrap}
.who-name{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:22px;letter-spacing:3px;
  color:var(--gold);text-shadow:0 0 18px rgba(233,196,106,.35)}
.who-account{font-size:12px;color:var(--muted);letter-spacing:1px}

.realm-zone{display:flex;align-items:center;gap:16px;flex-wrap:wrap;margin:18px 0 20px}
.stage-tag{font-size:12px;letter-spacing:3px;color:var(--jade);padding:5px 13px;border-radius:999px;
  background:rgba(45,212,191,.10);border:1px solid var(--line)}
.realm-name{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:56px;letter-spacing:10px;
  font-weight:900;line-height:1;
  background:linear-gradient(90deg,var(--jade),#fff,var(--gold),var(--jade));
  background-size:200% auto;-webkit-background-clip:text;background-clip:text;color:transparent;
  filter:drop-shadow(0 0 22px rgba(45,212,191,.35));animation:shine 5s linear infinite}
@keyframes shine{to{background-position:220% center}}
.title-badge{font-size:14px;letter-spacing:3px;color:var(--gold);padding:7px 16px;border-radius:999px;
  background:linear-gradient(135deg,rgba(233,196,106,.2),rgba(233,196,106,.06));
  border:1px solid rgba(233,196,106,.55);text-shadow:0 0 12px rgba(233,196,106,.5)}
.apex-badge{font-size:13px;letter-spacing:3px;color:#04130f;padding:7px 16px;border-radius:999px;
  background:linear-gradient(135deg,var(--gold),#f7f3e8);font-weight:900}

.progress{margin:4px 0 26px}
.progress-info{display:flex;justify-content:space-between;font-size:13px;color:var(--muted);
  letter-spacing:1px;margin-bottom:8px}
.progress-bar{height:12px;border-radius:999px;background:rgba(7,11,22,.8);
  border:1px solid var(--line);overflow:hidden;position:relative}
.progress-fill{height:100%;border-radius:999px;
  background:linear-gradient(90deg,var(--jade-deep),var(--jade),#5eead4);
  box-shadow:0 0 18px rgba(45,212,191,.6);transition:width .6s ease}
.progress.apex p{font-size:14px;color:var(--gold);letter-spacing:2px;line-height:1.8}

/* ===== 行动区 ===== */
.actions{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}
.action-card{padding:22px;border-radius:18px;background:var(--glass);
  border:1px solid rgba(255,255,255,.10);backdrop-filter:blur(10px);transition:.2s}
.action-card:hover{transform:translateY(-3px);border-color:var(--line);
  box-shadow:0 0 26px rgba(45,212,191,.15)}
.ac-head{display:flex;align-items:center;gap:10px;margin-bottom:12px}
.ac-ico{font-size:20px;color:var(--jade);text-shadow:0 0 14px var(--jade)}
.ac-head h3{font-size:17px;letter-spacing:3px;font-weight:900;
  font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.ac-desc{font-size:13px;color:var(--muted);line-height:1.9;letter-spacing:.5px;min-height:48px}
.ac-desc b{color:var(--gold)}
.ac-btn{width:100%;margin-top:14px;padding:11px;border:none;border-radius:11px;cursor:pointer;
  font-family:inherit;font-size:14px;font-weight:700;letter-spacing:5px;transition:.18s}
.ac-btn.jade{color:#04130f;background:linear-gradient(135deg,var(--jade),#5eead4);
  box-shadow:0 10px 26px rgba(45,212,191,.35)}
.ac-btn.teal{color:#04130f;background:linear-gradient(135deg,#5eead4,var(--jade));
  box-shadow:0 10px 26px rgba(45,212,191,.35)}
.ac-btn.gold{color:#04130f;background:linear-gradient(135deg,var(--gold),#f7f3e8);
  box-shadow:0 10px 26px rgba(233,196,106,.3);width:auto;padding:11px 22px;margin-top:14px}
.ac-btn:hover:not(:disabled){filter:brightness(1.1);transform:translateY(-1px)}
.ac-btn:disabled{opacity:.45;cursor:not-allowed;box-shadow:none}
.ac-meter{height:6px;border-radius:999px;background:rgba(7,11,22,.8);border:1px solid var(--line);
  overflow:hidden;margin-top:6px}
.ac-meter i{display:block;height:100%;background:linear-gradient(90deg,var(--gold),#f7f3e8);
  border-radius:999px;transition:width .4s}
.spar-form{display:flex;gap:10px}
.spar-form input{flex:1;min-width:0;padding:11px 14px;border-radius:11px;outline:none;
  background:rgba(7,11,22,.7);border:1px solid var(--line);color:var(--text);
  font-family:inherit;font-size:13px;letter-spacing:1px;transition:.15s;margin-top:14px}
.spar-form input:focus{border-color:var(--jade);box-shadow:0 0 0 3px rgba(45,212,191,.18)}
.spar-form input::placeholder{color:var(--muted)}

/* ===== 加载态 ===== */
.loading{text-align:center;padding:110px 26px;color:var(--muted);position:relative;z-index:2}
.loading-mark{font-size:54px;color:var(--line);margin-bottom:14px}
.loading p{letter-spacing:2px;line-height:2}
.retry{margin-top:18px;padding:8px 22px;border:1px solid var(--line);border-radius:999px;
  background:none;color:var(--jade);cursor:pointer;font-family:inherit;font-size:13px;letter-spacing:2px}
.retry:hover{box-shadow:0 0 16px rgba(45,212,191,.25)}

/* ===== 主内容 ===== */
.main{max-width:1240px;margin:40px auto 0;padding:0 26px 60px;position:relative;z-index:2}
.block{margin-bottom:46px}
.block-head{display:flex;align-items:center;gap:14px;margin-bottom:22px}
.bh-icon{font-size:22px;color:var(--jade);text-shadow:0 0 14px var(--jade)}
.block-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:26px;
  letter-spacing:4px;font-weight:900}
.bh-line{flex:1;height:1px;background:linear-gradient(90deg,var(--line),transparent)}
.bh-tip{font-size:12px;color:var(--muted);letter-spacing:1px}

/* 境界阶梯 */
.ladder{display:flex;gap:22px;flex-wrap:wrap}
.stage-group{flex:1;min-width:420px;padding:20px 22px;border-radius:18px;background:var(--glass);
  border:1px solid rgba(255,255,255,.10);backdrop-filter:blur(10px)}
.sg-title{font-size:13px;letter-spacing:4px;color:var(--jade);margin-bottom:14px;
  padding-bottom:10px;border-bottom:1px dashed var(--line)}
.sg-realms{display:flex;flex-wrap:wrap;gap:10px}
.realm-chip{display:flex;align-items:center;gap:8px;padding:8px 14px;border-radius:999px;
  font-size:13px;letter-spacing:1px;color:var(--muted);
  background:rgba(7,11,22,.55);border:1px solid rgba(255,255,255,.10);transition:.2s}
.realm-chip.past{opacity:.55}
.realm-chip.now{color:#04130f;font-weight:900;letter-spacing:2px;
  background:linear-gradient(135deg,var(--jade),#5eead4);border-color:transparent;
  box-shadow:0 0 22px rgba(45,212,191,.5)}
.rc-idx{font-size:11px;opacity:.7}
.rc-title{font-size:11px;color:var(--gold);letter-spacing:1px;
  padding:2px 8px;border-radius:999px;background:rgba(233,196,106,.14);
  border:1px solid rgba(233,196,106,.4)}
.realm-chip.now .rc-title{color:#04130f;background:rgba(7,11,22,.18);border-color:rgba(7,11,22,.3)}
.rc-mark{font-size:12px}
.now-mark{font-weight:900}

/* 功法阁 */
.gongfa-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(270px,1fr));gap:18px}
.gongfa-card{position:relative;padding:22px 20px 18px;border-radius:18px;overflow:hidden;
  background:var(--glass);border:1px solid rgba(255,255,255,.10);backdrop-filter:blur(12px);
  box-shadow:0 18px 44px rgba(0,0,0,.42);transition:.2s}
.gongfa-card::before{content:"";position:absolute;left:0;top:0;bottom:0;width:3px;
  background:linear-gradient(180deg,var(--jade),transparent);opacity:.85}
.gongfa-card.owned{border-color:rgba(233,196,106,.45)}
.gongfa-card.owned::before{background:linear-gradient(180deg,var(--gold),transparent)}
.gongfa-card:hover{transform:translateY(-5px);border-color:var(--jade);
  box-shadow:0 26px 60px rgba(0,0,0,.55),0 0 26px rgba(45,212,191,.2)}
.gf-top{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}
.gf-tag{font-size:11px;letter-spacing:2px;padding:4px 10px;border-radius:999px;
  color:var(--jade);background:rgba(45,212,191,.12);border:1px solid var(--line)}
.gf-owned{font-size:11px;letter-spacing:2px;color:var(--gold)}
.gf-name{font-size:20px;font-weight:900;letter-spacing:2px;margin-bottom:10px;
  font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.gf-desc{font-size:13px;color:var(--muted);line-height:1.9;letter-spacing:.5px;
  min-height:62px;display:-webkit-box;-webkit-line-clamp:3;-webkit-box-orient:vertical;overflow:hidden}
.gf-foot{display:flex;align-items:center;justify-content:space-between;margin-top:14px;gap:10px}
.gf-meta{display:flex;flex-direction:column;gap:4px}
.gf-price{font-size:13px;color:var(--gold);letter-spacing:1px}
.gf-gain{font-size:12px;color:var(--jade);letter-spacing:1px}
.gf-buy{padding:9px 20px;border:none;border-radius:10px;cursor:pointer;
  font-family:inherit;font-size:13px;font-weight:700;letter-spacing:4px;color:#04130f;
  background:linear-gradient(135deg,var(--jade),#5eead4);transition:.15s;flex-shrink:0}
.gf-buy:hover:not(:disabled){filter:brightness(1.1)}
.gf-buy:disabled{opacity:.4;cursor:not-allowed}
.gf-done{font-size:13px;color:var(--gold);letter-spacing:2px;flex-shrink:0}

.foot{position:relative;z-index:2;text-align:center;padding:26px;font-size:12px;
  color:var(--muted);letter-spacing:1px;border-top:1px solid rgba(255,255,255,.06)}

/* ===== 突破特效 ===== */
.break-flash{position:fixed;inset:0;z-index:90;display:grid;place-items:center;
  background:rgba(4,7,14,.72);backdrop-filter:blur(8px)}
.bf-card{text-align:center;position:relative;padding:56px 80px;border-radius:24px;
  background:linear-gradient(160deg,rgba(14,22,42,.96),rgba(7,11,22,.96));
  border:1px solid rgba(233,196,106,.5);animation:cardIn .5s ease both}
@keyframes cardIn{from{opacity:0;transform:scale(.9)}to{opacity:1;transform:none}}
.bf-glow{position:absolute;inset:-30px;border-radius:50%;pointer-events:none;
  background:radial-gradient(circle,rgba(233,196,106,.35),transparent 65%);
  animation:glowPulse 1.2s ease-in-out infinite}
@keyframes glowPulse{50%{transform:scale(1.12);opacity:.7}}
.bf-from{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:26px;color:var(--muted);letter-spacing:6px}
.bf-arrow{display:block;font-size:30px;color:var(--gold);margin:10px 0;
  text-shadow:0 0 18px rgba(233,196,106,.7);animation:arrowDown 1s ease infinite}
@keyframes arrowDown{50%{transform:translateY(6px)}}
.bf-to{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:52px;font-weight:900;
  letter-spacing:10px;background:linear-gradient(90deg,var(--gold),#fff,var(--gold));
  -webkit-background-clip:text;background-clip:text;color:transparent;
  filter:drop-shadow(0 0 26px rgba(233,196,106,.6))}
.bf-title{margin-top:16px;font-size:18px;color:var(--gold);letter-spacing:4px}

/* ===== toast ===== */
.toast{position:fixed;left:50%;top:84px;transform:translateX(-50%) translateY(-20px);
  background:linear-gradient(135deg,var(--jade-deep),var(--jade));
  color:#04130f;padding:12px 22px;border-radius:12px;font-weight:700;
  letter-spacing:1px;opacity:0;pointer-events:none;transition:.3s;z-index:95;
  box-shadow:0 14px 40px rgba(45,212,191,.45);max-width:80vw}
.toast.show{opacity:1;transform:translateX(-50%) translateY(0)}

.fade-enter-active,.fade-leave-active{transition:opacity .35s}
.fade-enter-from,.fade-leave-to{opacity:0}

@media(max-width:980px){.actions{grid-template-columns:1fr}.stage-group{min-width:100%}}
@media(max-width:880px){.crumbs{display:none}.realm-name{font-size:40px;letter-spacing:6px}}
</style>
