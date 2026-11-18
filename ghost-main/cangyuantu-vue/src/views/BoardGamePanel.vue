<template>
  <div class="boardgame" ref="root">
    <div class="embers">
      <i v-for="(e, i) in embers" :key="i" :style="e"></i>
    </div>

    <!-- ===== 顶部栏 ===== -->
    <header class="nav">
      <div class="nav-inner">
        <div class="brand" @click="$emit('go-home')">
          <div class="seal">弈</div>
          <div class="brand-txt">
            <b>太虚弈局</b>
            <span>TAI XU YI JU · 桌游</span>
          </div>
        </div>
        <nav class="crumbs">
          <a @click="$emit('go-home')">首页</a>
          <span class="sep">›</span>
          <span class="cur">太虚弈局</span>
        </nav>
        <div class="nav-right" v-if="bg && bg.asset">
          <span class="pill power">⚡ 神尊力 {{ bg.asset.power }}</span>
          <span class="pill copper">◉ 铜币 {{ bg.asset.copper }}</span>
          <button class="back" @click="$emit('go-home')">‹ 返回首页</button>
        </div>
      </div>
    </header>

    <!-- 加载/异常态 -->
    <section class="loading" v-if="!bg">
      <div class="loading-mark">◇</div>
      <p>{{ loadMsg }}</p>
      <button class="retry" @click="load" v-if="loadFailed">重新探查</button>
    </section>

    <template v-if="bg">
      <!-- ===== 状态条 ===== -->
      <section class="statusbar">
        <div class="sb-item">
          <span class="sb-ico">◈</span>
          <div><b>{{ bg.daily_left }}</b><small>今日余局 / {{ bg.daily_limit }}</small></div>
        </div>
        <div class="sb-item">
          <span class="sb-ico">◉</span>
          <div><b>{{ bg.entry_fee }}</b><small>入局报名铜币</small></div>
        </div>
        <div class="sb-item">
          <span class="sb-ico">♛</span>
          <div><b>{{ bg.total_won }}</b><small>累计登顶 / {{ bg.total_played }} 局</small></div>
        </div>
        <div class="sb-item">
          <span class="sb-ico">⚄</span>
          <div><b>{{ bg.max_rolls }}</b><small>每局最多掷骰</small></div>
        </div>
      </section>

      <!-- ===== 主区：棋盘 + 操作 ===== -->
      <main class="arena">
        <section class="board-wrap">
          <div class="board-head">
            <h2>太虚棋局</h2>
            <span>掷骰行棋 · 落格得宝 · 登临神位</span>
          </div>

          <div class="board">
            <div v-for="cell in bg.board" :key="cell.index"
                 class="cell"
                 :class="['t-' + cell.type.toLowerCase(), {
                   here: cur && cur.position === cell.index,
                   finish: cell.type === 'FINISH'
                 }]">
              <span class="cell-idx">{{ cell.index }}</span>
              <span class="cell-icon">{{ cell.icon }}</span>
              <span class="cell-name">{{ cell.name }}</span>
              <span class="cell-gain" v-if="cell.copper">◉{{ cell.copper }}</span>
              <span class="cell-gain pw" v-if="cell.power">⚡{{ cell.power }}</span>
              <span class="pawn" v-if="cur && cur.position === cell.index">我</span>
            </div>
          </div>

          <!-- 图例 -->
          <div class="legend">
            <span><i class="dot c-copper"></i>铜币</span>
            <span><i class="dot c-power"></i>神尊力</span>
            <span><i class="dot c-boon"></i>双得</span>
            <span><i class="dot c-fortune"></i>福缘</span>
            <span><i class="dot c-peril"></i>劫难</span>
            <span><i class="dot c-finish"></i>终点</span>
          </div>
        </section>

        <!-- 操作面板 -->
        <aside class="side">
          <div class="side-card play-card">
            <template v-if="cur && cur.status === 'playing'">
              <div class="play-head">
                <span class="live-dot"></span> 对局进行中
              </div>
              <div class="play-progress">
                <div class="pp-bar"><i :style="{ width: Math.round(cur.position / (bg.board_size - 1) * 100) + '%' }"></i></div>
                <div class="pp-meta">
                  <span>第 {{ cur.rolls }} / {{ bg.max_rolls }} 掷</span>
                  <span>距神位 {{ Math.max(0, bg.board_size - 1 - cur.position) }} 格</span>
                </div>
              </div>
              <div class="dice" :class="{ rolling: rolling }">{{ rolling ? '⚄' : diceFace }}</div>
              <button class="big-btn" :disabled="busy || rolling" @click="doRoll">
                {{ rolling ? '骰子转动中…' : '掷 骰 行 棋' }}
              </button>
              <p class="play-tip">本局已得 ◉{{ cur.gain_copper }} 铜币 · ⚡{{ cur.gain_power }} 神尊力</p>
            </template>

            <template v-else>
              <div class="play-head ready">太虚弈局</div>
              <p class="intro">
                掷骰行棋，落格即可收获铜币与神尊力；抵达终点「登临神位」通关，
                另得 <b>◉150 铜币</b> 与 <b>⚡100 神尊力</b> 大奖。
              </p>
              <p class="cost-line">入局需 <b>◉ {{ bg.entry_fee }} 铜币</b> 报名</p>
              <button class="big-btn" :disabled="busy || bg.daily_left <= 0 || bg.asset.copper < bg.entry_fee"
                      @click="doStart">
                {{ bg.daily_left <= 0 ? '今日次数已尽' : (bg.asset.copper < bg.entry_fee ? '铜币不足' : '入 局 开 弈') }}
              </button>
              <p class="play-tip">每局最多 {{ bg.max_rolls }} 次掷骰 · 每日 {{ bg.daily_limit }} 局</p>
            </template>
          </div>

          <!-- 最近一掷结果 -->
          <transition name="pop">
            <div class="side-card result-card" v-if="lastRoll">
              <div class="rc-head">上一掷 · {{ lastRoll.dice }} 点</div>
              <div class="rc-body">
                <span class="rc-cell">{{ lastRoll.cell_icon }} {{ lastRoll.cell_name }}</span>
                <span class="rc-gains">
                  <em v-if="lastRoll.gain_copper" :class="lastRoll.gain_copper < 0 ? 'minus' : ''">
                    {{ lastRoll.gain_copper > 0 ? '+' : '' }}{{ lastRoll.gain_copper }} 铜币
                  </em>
                  <em v-if="lastRoll.gain_power" :class="lastRoll.gain_power < 0 ? 'minus' : ''">
                    {{ lastRoll.gain_power > 0 ? '+' : '' }}{{ lastRoll.gain_power }} 神尊力
                  </em>
                  <em v-if="!lastRoll.gain_copper && !lastRoll.gain_power">无事发生</em>
                </span>
              </div>
            </div>
          </transition>

          <!-- 战绩 -->
          <div class="side-card history-card">
            <div class="hc-head">近期战绩</div>
            <ul v-if="bg.history && bg.history.length">
              <li v-for="g in bg.history.slice(0, 6)" :key="g.id">
                <span class="hc-mark" :class="{ win: g.pass }">{{ g.pass ? '♛' : '·' }}</span>
                <span class="hc-info">{{ g.pass ? '登临神位' : '行棋未竟' }} · {{ g.rolls }} 掷</span>
                <span class="hc-gain">◉{{ g.gain_copper }} ⚡{{ g.gain_power }}</span>
              </li>
            </ul>
            <p class="hc-empty" v-else>尚无战绩，入局开弈</p>
          </div>
        </aside>
      </main>

      <!-- ===== 成就 + 排行榜 ===== -->
      <main class="extras" v-if="bg">
        <section class="block achieve-block">
          <div class="block-head">
            <span class="bh-icon">✧</span>
            <h2>成就殿堂</h2>
            <span class="bh-line"></span>
            <span class="bh-tip">行棋问道，步步留痕</span>
          </div>
          <div class="badge-grid">
            <div v-for="a in bg.achievements" :key="a.name" class="badge"
                 :class="{ locked: !a.unlocked }">
              <span class="badge-mark">{{ a.unlocked ? '✦' : '◇' }}</span>
              <div class="badge-info">
                <b>{{ a.name }}</b>
                <small>{{ a.desc }}</small>
              </div>
              <span class="badge-state">{{ a.unlocked ? '已达成' : '未解锁' }}</span>
            </div>
          </div>
        </section>

        <section class="block rank-block">
          <div class="block-head">
            <span class="bh-icon">♛</span>
            <h2>太虚榜</h2>
            <span class="bh-line"></span>
            <span class="bh-tip">群雄逐鹿，谁登神位</span>
          </div>
          <div class="rank-tabs">
            <button :class="{ on: rankTab === 'wins' }" @click="rankTab = 'wins'">登顶榜</button>
            <button :class="{ on: rankTab === 'copper' }" @click="rankTab = 'copper'">财富榜</button>
          </div>
          <div class="rank-list" v-if="rankRows.length">
            <div v-for="(r, i) in rankRows" :key="r.name + i" class="rank-row" :class="'rk-' + (i + 1)">
              <span class="rk-no">{{ i < 3 ? ['♛','♞','♜'][i] : (i + 1) }}</span>
              <span class="rk-name">{{ r.name }}</span>
              <span class="rk-stat" v-if="rankTab === 'wins'">{{ r.wins }} 次登顶</span>
              <span class="rk-stat" v-else>{{ r.copper }} 铜币</span>
              <span class="rk-gain">⚡{{ r.power }} · ◉{{ r.copper }}</span>
            </div>
          </div>
          <p class="rank-empty" v-else>榜上无名，入局一较高下</p>
        </section>
      </main>

      <footer class="foot">沧元图 · 太虚弈局 —— 掷骰问道 · 落格得宝 · 登临神位</footer>
    </template>

    <!-- 通关特效 -->
    <transition name="fade">
      <div class="win-flash" v-if="winFx">
        <div class="wf-card">
          <div class="wf-glow"></div>
          <p class="wf-crown">♛</p>
          <p class="wf-title">登 临 神 位</p>
          <p class="wf-sub">本局收获</p>
          <p class="wf-gain">◉ {{ winFx.gain_copper }} 铜币　⚡ {{ winFx.gain_power }} 神尊力</p>
        </div>
      </div>
    </transition>

    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { fetchBoardGame, startBoardGame, rollBoardGame, fetchBoardGameRank } from '../api'

const props = defineProps({ avatar: { type: Object, default: null } })
defineEmits(['go-home'])

const root = ref(null)

const palette = ['#2dd4bf', '#e9c46a', '#ef4454', '#5eead4', '#ffffff']
const embers = Array.from({ length: 18 }, () => {
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

const bg = ref(null)
const busy = ref(false)
const rolling = ref(false)
const diceFace = ref('⚄')
const loadFailed = ref(false)
const loadMsg = ref('正在推演棋局…')
const lastRoll = ref(null)
const winFx = ref(null)
const rank = ref(null)
const rankTab = ref('wins')

const cur = computed(() => bg.value && bg.value.current)
const rankRows = computed(() => {
  if (!rank.value) return []
  if (rankTab.value === 'wins') return rank.value.by_wins || []
  return (rank.value.by_copper || []).map(r => ({ ...r, wins: '—' }))
})

function cred() {
  const a = props.avatar || {}
  return { name: a.deity_name, spell: a.spell }
}

async function load() {
  const a = props.avatar || {}
  if (!a.deity_name) { loadMsg.value = '游客无根基，请先觉醒神尊'; loadFailed.value = true; return }
  loadFailed.value = false
  loadMsg.value = '正在推演棋局…'
  bg.value = null
  const d = await fetchBoardGame(a.deity_name)
  if (d && d.ok && d.boardgame) {
    bg.value = d.boardgame
  } else {
    loadFailed.value = true
    loadMsg.value = (d && d.msg) || '无法连接元界（后端未启动？）'
  }
  const r = await fetchBoardGameRank()
  if (r && r.ok) rank.value = r
}
onMounted(load)

async function doStart() {
  if (busy.value) return
  busy.value = true
  const d = await startBoardGame(cred())
  busy.value = false
  if (d.ok) {
    if (d.asset) bg.value.asset = d.asset
    bg.value.current = d.game
    lastRoll.value = null
    toast(d.resumed ? '继续未完的弈局' : '入局开弈，掷骰问道')
    const s = await fetchBoardGame((props.avatar || {}).deity_name)
    if (s && s.ok && s.boardgame) bg.value = s.boardgame
  } else toast(d.msg || '入局失败')
}

async function doRoll() {
  if (busy.value || rolling.value) return
  busy.value = true
  rolling.value = true
  // 骰子翻滚动画
  const faces = ['⚀', '⚁', '⚂', '⚃', '⚄', '⚅']
  let ticks = 0
  const timer = setInterval(() => {
    diceFace.value = faces[Math.floor(Math.random() * 6)]
    ticks++
    if (ticks > 14) clearInterval(timer)
  }, 70)

  const d = await rollBoardGame(cred())
  await new Promise(r => setTimeout(r, 900))
  clearInterval(timer)
  rolling.value = false
  busy.value = false

  if (d.ok) {
    diceFace.value = faces[(d.dice || 1) - 1]
    lastRoll.value = d
    if (d.asset) bg.value.asset = d.asset
    if (d.game) bg.value.current = d.game
    if (d.pass === 1) {
      winFx.value = { gain_copper: d.game.gain_copper, gain_power: d.game.gain_power }
      setTimeout(() => { winFx.value = null }, 3000)
    }
    const g = d.gain_copper ? `${d.gain_copper > 0 ? '+' : ''}${d.gain_copper} 铜币 ` : ''
    const p = d.gain_power ? `${d.gain_power > 0 ? '+' : ''}${d.gain_power} 神尊力` : ''
    toast(d.finished
      ? (d.pass === 1 ? '登临神位！' : '行棋已尽')
      : `掷出 ${d.dice} 点，落「${d.cell_name}」${g ? ' ' + g : ''}${p ? ' ' + p : ''}`)
    const s = await fetchBoardGame((props.avatar || {}).deity_name)
    if (s && s.ok && s.boardgame) bg.value = s.boardgame
  } else {
    toast(d.msg || '掷骰失败')
  }
}

const toastMsg = ref('')
const toastShow = ref(false)
let toastTimer = null
function toast(msg) {
  toastMsg.value = msg
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 3000)
}
</script>

<style scoped>
.boardgame{position:fixed;inset:0;overflow-y:auto;overflow-x:hidden;
  background:
    radial-gradient(120% 80% at 80% 0%,rgba(233,196,106,.10),transparent 55%),
    radial-gradient(100% 90% at 10% 100%,rgba(45,212,191,.10),transparent 55%),
    linear-gradient(180deg,var(--ink2),var(--ink) 70%);
  color:var(--text)}
.embers{position:fixed;inset:0;pointer-events:none;z-index:0;overflow:hidden}
.embers i{position:absolute;bottom:-10px;border-radius:50%;opacity:0}
@keyframes rise{0%{transform:translateY(0) scale(1);opacity:0}10%{opacity:.9}90%{opacity:.5}100%{transform:translateY(-105vh) scale(.4);opacity:0}}

.nav{position:sticky;top:0;z-index:30;background:linear-gradient(180deg,rgba(7,11,22,.92),rgba(7,11,22,.62));
  backdrop-filter:blur(14px);border-bottom:1px solid var(--line);box-shadow:0 8px 30px rgba(0,0,0,.4)}
.nav-inner{max-width:1280px;margin:0 auto;display:flex;align-items:center;gap:26px;padding:14px 26px}
.brand{display:flex;align-items:center;gap:12px;cursor:pointer;flex-shrink:0}
.seal{width:42px;height:42px;border-radius:11px;background:linear-gradient(135deg,var(--jade),#0f766e);
  display:grid;place-items:center;font-weight:900;color:#04130f;font-size:20px;box-shadow:0 0 22px rgba(45,212,191,.5)}
.brand-txt b{font-size:18px;letter-spacing:4px;font-weight:900;display:block}
.brand-txt span{font-size:11px;color:var(--muted);letter-spacing:2px}
.crumbs{display:flex;align-items:center;gap:10px;flex:1;font-size:13px;letter-spacing:1px}
.crumbs a{color:var(--muted);cursor:pointer;transition:.15s}
.crumbs a:hover{color:var(--jade)}
.crumbs .sep{color:var(--line)}
.crumbs .cur{color:var(--gold)}
.nav-right{display:flex;align-items:center;gap:10px;flex-shrink:0}
.pill{padding:6px 14px;border-radius:999px;font-size:13px;letter-spacing:1px;font-weight:700;border:1px solid var(--line)}
.pill.power{color:var(--jade);background:rgba(45,212,191,.10)}
.pill.copper{color:var(--gold);background:rgba(233,196,106,.10);border-color:rgba(233,196,106,.4)}
.back{padding:6px 14px;border:1px solid var(--line);border-radius:999px;background:none;color:var(--muted);
  cursor:pointer;font-family:inherit;font-size:12px;letter-spacing:2px;transition:.15s}
.back:hover{color:var(--jade);border-color:var(--jade)}

.loading{text-align:center;padding:120px 26px;color:var(--muted);position:relative;z-index:2}
.loading-mark{font-size:54px;color:var(--line);margin-bottom:14px}
.loading p{letter-spacing:2px;line-height:2}
.retry{margin-top:18px;padding:8px 22px;border:1px solid var(--line);border-radius:999px;background:none;
  color:var(--jade);cursor:pointer;font-family:inherit;font-size:13px;letter-spacing:2px}

.statusbar{max-width:1280px;margin:26px auto 0;padding:0 26px;display:grid;grid-template-columns:repeat(4,1fr);gap:14px;position:relative;z-index:2}
.sb-item{display:flex;align-items:center;gap:14px;padding:16px 20px;border-radius:16px;background:var(--glass);
  border:1px solid rgba(255,255,255,.10);backdrop-filter:blur(10px)}
.sb-ico{font-size:24px;color:var(--jade);text-shadow:0 0 14px var(--jade)}
.sb-item b{font-size:22px;font-weight:900;color:var(--gold);display:block;line-height:1.2}
.sb-item small{font-size:12px;color:var(--muted);letter-spacing:1px}

.arena{max-width:1280px;margin:22px auto 0;padding:0 26px 40px;display:grid;grid-template-columns:1fr 340px;gap:22px;position:relative;z-index:2}
.board-wrap{padding:24px;border-radius:22px;background:linear-gradient(160deg,rgba(14,22,42,.8),rgba(7,11,22,.6));
  border:1px solid rgba(255,255,255,.12);backdrop-filter:blur(12px);box-shadow:0 22px 60px rgba(0,0,0,.45)}
.board-head{display:flex;align-items:baseline;gap:16px;margin-bottom:20px}
.board-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:24px;letter-spacing:4px;font-weight:900;color:var(--gold)}
.board-head span{font-size:12px;color:var(--muted);letter-spacing:2px}

.board{display:grid;grid-template-columns:repeat(8,1fr);gap:10px}
.cell{position:relative;min-height:104px;border-radius:14px;padding:10px 6px;display:flex;flex-direction:column;
  align-items:center;justify-content:center;gap:3px;background:rgba(7,11,22,.6);border:1px solid rgba(255,255,255,.10);
  transition:.25s;overflow:hidden}
.cell::before{content:"";position:absolute;inset:0;opacity:.10}
.cell.t-copper::before{background:var(--gold)}
.cell.t-power::before{background:var(--jade)}
.cell.t-boon::before{background:linear-gradient(135deg,var(--jade),var(--gold))}
.cell.t-fortune::before{background:#a78bfa}
.cell.t-peril::before{background:var(--crimson)}
.cell.t-finish{background:linear-gradient(135deg,rgba(233,196,106,.22),rgba(233,196,106,.05));border-color:rgba(233,196,106,.6)}
.cell.t-start{background:rgba(45,212,191,.10)}
.cell:not(.here):hover{transform:translateY(-2px);border-color:rgba(255,255,255,.28)}
.cell.here{transform:translateY(-6px) scale(1.03);border-color:var(--jade);
  box-shadow:0 0 0 2px var(--jade),0 0 30px rgba(45,212,191,.5),0 14px 34px rgba(45,212,191,.3);z-index:3}
.cell-idx{position:absolute;top:6px;left:8px;font-size:10px;color:var(--muted);opacity:.7}
.cell-icon{font-size:26px;line-height:1;text-shadow:0 0 12px currentColor}
.t-copper .cell-icon,.t-finish .cell-icon{color:var(--gold)}
.t-power .cell-icon,.t-start .cell-icon{color:var(--jade)}
.t-boon .cell-icon{color:#5eead4}
.t-fortune .cell-icon{color:#c4b5fd}
.t-peril .cell-icon{color:#f87171}
.cell-name{font-size:12px;letter-spacing:1px;color:var(--text);font-weight:700}
.cell-gain{font-size:11px;color:var(--gold)}
.cell-gain.pw{color:var(--jade)}
.pawn{position:absolute;top:-8px;right:-6px;width:24px;height:24px;border-radius:50%;
  background:linear-gradient(135deg,var(--crimson),#b91c1c);color:#fff;font-size:11px;font-weight:900;
  display:grid;place-items:center;box-shadow:0 0 16px rgba(239,68,84,.7);animation:pawnPulse 1.3s ease-in-out infinite}
@keyframes pawnPulse{50%{transform:scale(1.15)}}

.legend{display:flex;flex-wrap:wrap;gap:16px;margin-top:18px;padding-top:14px;border-top:1px dashed var(--line)}
.legend span{display:flex;align-items:center;gap:6px;font-size:12px;color:var(--muted);letter-spacing:1px}
.dot{width:9px;height:9px;border-radius:50%;display:inline-block}
.c-copper{background:var(--gold)}.c-power{background:var(--jade)}.c-boon{background:linear-gradient(135deg,var(--jade),var(--gold))}
.c-fortune{background:#a78bfa}.c-peril{background:var(--crimson)}.c-finish{background:#f7f3e8}

.side{display:flex;flex-direction:column;gap:16px}
.side-card{padding:20px;border-radius:18px;background:var(--glass);border:1px solid rgba(255,255,255,.10);backdrop-filter:blur(12px)}
.play-card{text-align:center}
.play-head{font-size:15px;letter-spacing:3px;font-weight:900;color:var(--jade);margin-bottom:14px;display:flex;align-items:center;justify-content:center;gap:8px}
.play-head.ready{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:22px;color:var(--gold)}
.live-dot{width:8px;height:8px;border-radius:50%;background:var(--jade);box-shadow:0 0 10px var(--jade);animation:pawnPulse 1.3s infinite}
.play-progress{margin-bottom:14px}
.pp-bar{height:9px;border-radius:999px;background:rgba(7,11,22,.8);border:1px solid var(--line);overflow:hidden}
.pp-bar i{display:block;height:100%;border-radius:999px;background:linear-gradient(90deg,var(--jade-deep),var(--jade),#5eead4);transition:width .5s}
.pp-meta{display:flex;justify-content:space-between;margin-top:7px;font-size:11px;color:var(--muted);letter-spacing:1px}
.dice{font-size:72px;line-height:1.1;color:var(--gold);text-shadow:0 0 30px rgba(233,196,106,.6),0 0 60px rgba(233,196,106,.25);margin:6px 0;
  filter:drop-shadow(0 6px 10px rgba(0,0,0,.5))}
.dice.rolling{animation:shake .2s linear infinite}
@keyframes shake{0%{transform:rotate(-12deg)}50%{transform:rotate(12deg)}100%{transform:rotate(-12deg)}}
.big-btn{width:100%;margin-top:10px;padding:13px;border:none;border-radius:12px;cursor:pointer;font-family:inherit;
  font-size:15px;font-weight:900;letter-spacing:5px;color:#04130f;background:linear-gradient(135deg,var(--jade),#5eead4);
  box-shadow:0 10px 26px rgba(45,212,191,.35);transition:.18s}
.big-btn:hover:not(:disabled){filter:brightness(1.1);transform:translateY(-1px)}
.big-btn:disabled{opacity:.45;cursor:not-allowed;box-shadow:none}
.intro{font-size:13px;line-height:1.9;color:var(--muted);letter-spacing:.5px;text-align:left;margin:6px 0 12px}
.intro b{color:var(--gold)}
.cost-line{font-size:13px;color:var(--text);letter-spacing:1px;margin-bottom:6px}
.cost-line b{color:var(--gold)}
.play-tip{font-size:11px;color:var(--muted);letter-spacing:1px;margin-top:12px}

.rc-head{font-size:12px;color:var(--jade);letter-spacing:2px;margin-bottom:10px}
.rc-body{display:flex;flex-direction:column;gap:6px}
.rc-cell{font-size:15px;font-weight:700;letter-spacing:1px}
.rc-gains{display:flex;flex-wrap:wrap;gap:10px}
.rc-gains em{font-style:normal;font-size:13px;color:var(--gold)}
.rc-gains em.minus{color:#f87171}
.result-card{border-color:rgba(45,212,191,.35)}

.hc-head{font-size:13px;color:var(--gold);letter-spacing:2px;margin-bottom:10px}
.history-card ul{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:8px}
.history-card li{display:flex;align-items:center;gap:8px;font-size:12px;color:var(--muted)}
.hc-mark{width:18px;text-align:center;color:var(--muted)}
.hc-mark.win{color:var(--gold)}
.hc-info{flex:1}
.hc-gain{color:var(--jade);font-size:11px;white-space:nowrap}
.hc-empty{font-size:12px;color:var(--muted);letter-spacing:1px}

/* ===== 成就 + 排行榜 ===== */
.extras{max-width:1280px;margin:4px auto 0;padding:0 26px 40px;display:grid;grid-template-columns:1fr 1fr;gap:22px;position:relative;z-index:2}
.block{padding:24px;border-radius:22px;background:linear-gradient(160deg,rgba(14,22,42,.8),rgba(7,11,22,.6));
  border:1px solid rgba(255,255,255,.12);backdrop-filter:blur(12px)}
.block-head{display:flex;align-items:baseline;gap:14px;margin-bottom:20px}
.bh-icon{font-size:20px;color:var(--jade);text-shadow:0 0 12px var(--jade)}
.block-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:22px;letter-spacing:4px;font-weight:900;color:var(--gold)}
.bh-line{flex:1;height:1px;background:linear-gradient(90deg,var(--line),transparent)}
.bh-tip{font-size:11px;color:var(--muted);letter-spacing:1px}

.badge-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:12px}
.badge{display:flex;align-items:center;gap:12px;padding:14px;border-radius:14px;background:rgba(7,11,22,.55);
  border:1px solid rgba(233,196,106,.4);transition:.2s}
.badge:hover{transform:translateY(-3px);border-color:var(--gold)}
.badge.locked{opacity:.45;border-color:var(--line)}
.badge-mark{font-size:26px;color:var(--gold);text-shadow:0 0 14px rgba(233,196,106,.5)}
.badge.locked .badge-mark{color:var(--muted);text-shadow:none}
.badge-info{flex:1;min-width:0}
.badge-info b{display:block;font-size:14px;letter-spacing:1px}
.badge-info small{display:block;font-size:11px;color:var(--muted);margin-top:3px}
.badge-state{font-size:11px;color:var(--gold);letter-spacing:1px;white-space:nowrap}
.badge.locked .badge-state{color:var(--muted)}

.rank-tabs{display:flex;gap:8px;margin-bottom:14px}
.rank-tabs button{flex:1;padding:8px;border:1px solid var(--line);border-radius:10px;background:rgba(7,11,22,.6);
  color:var(--muted);font-family:inherit;font-size:13px;letter-spacing:2px;cursor:pointer;transition:.15s}
.rank-tabs button.on{color:#04130f;background:linear-gradient(135deg,var(--jade),#5eead4);border-color:transparent;font-weight:700}
.rank-list{display:flex;flex-direction:column;gap:7px}
.rank-row{display:flex;align-items:center;gap:10px;padding:9px 12px;border-radius:10px;background:rgba(7,11,22,.5);
  border:1px solid rgba(255,255,255,.06);font-size:12px;transition:.15s}
.rank-row:hover{border-color:var(--jade);transform:translateX(3px)}
.rank-row.rk-1{background:linear-gradient(90deg,rgba(233,196,106,.18),rgba(7,11,22,.5));border-color:rgba(233,196,106,.5)}
.rk-no{width:26px;text-align:center;font-size:16px;color:var(--gold);font-weight:900}
.rk-name{flex:1;color:var(--text);letter-spacing:1px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.rk-stat{color:var(--jade);white-space:nowrap}
.rk-gain{color:var(--muted);font-size:11px;white-space:nowrap}
.rank-empty{font-size:12px;color:var(--muted);letter-spacing:1px;text-align:center;padding:20px 0}

.foot{position:relative;z-index:2;text-align:center;padding:24px;font-size:12px;color:var(--muted);letter-spacing:1px;border-top:1px solid rgba(255,255,255,.06)}

.win-flash{position:fixed;inset:0;z-index:90;display:grid;place-items:center;background:rgba(4,7,14,.72);backdrop-filter:blur(8px)}
.wf-card{text-align:center;position:relative;padding:52px 76px;border-radius:24px;
  background:linear-gradient(160deg,rgba(14,22,42,.96),rgba(7,11,22,.96));border:1px solid rgba(233,196,106,.5);animation:cardIn .5s ease both}
@keyframes cardIn{from{opacity:0;transform:scale(.9)}to{opacity:1;transform:none}}
.wf-glow{position:absolute;inset:-30px;border-radius:50%;pointer-events:none;background:radial-gradient(circle,rgba(233,196,106,.35),transparent 65%);animation:glowPulse 1.2s ease-in-out infinite}
@keyframes glowPulse{50%{transform:scale(1.12);opacity:.7}}
.wf-crown{font-size:46px;color:var(--gold);text-shadow:0 0 24px rgba(233,196,106,.7);margin:0}
.wf-title{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:40px;font-weight:900;letter-spacing:10px;
  background:linear-gradient(90deg,var(--gold),#fff,var(--gold));-webkit-background-clip:text;background-clip:text;color:transparent;margin:8px 0}
.wf-sub{font-size:13px;color:var(--muted);letter-spacing:3px}
.wf-gain{font-size:17px;color:var(--gold);letter-spacing:2px;margin-top:6px}

.toast{position:fixed;left:50%;top:84px;transform:translateX(-50%) translateY(-20px);
  background:linear-gradient(135deg,var(--jade-deep),var(--jade));color:#04130f;padding:12px 22px;border-radius:12px;
  font-weight:700;letter-spacing:1px;opacity:0;pointer-events:none;transition:.3s;z-index:95;
  box-shadow:0 14px 40px rgba(45,212,191,.45);max-width:80vw}
.toast.show{opacity:1;transform:translateX(-50%) translateY(0)}
.pop-enter-active,.pop-leave-active{transition:.3s}
.pop-enter-from,.pop-leave-to{opacity:0;transform:translateY(-8px)}
.fade-enter-active,.fade-leave-active{transition:opacity .35s}
.fade-enter-from,.fade-leave-to{opacity:0}

@media(max-width:1040px){.arena{grid-template-columns:1fr}.statusbar{grid-template-columns:repeat(2,1fr)}.extras{grid-template-columns:1fr}}
@media(max-width:680px){.board{grid-template-columns:repeat(4,1fr)}.crumbs{display:none}.nav-right .pill{display:none}}
</style>
