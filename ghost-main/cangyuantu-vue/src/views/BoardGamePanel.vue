<template>
  <div class="boardgame" ref="root">
    <!-- 水墨 drifting 墨点 -->
    <div class="ink-drift"><i v-for="(e, i) in motes" :key="i" :style="e"></i></div>

    <!-- ===== 顶部栏 ===== -->
    <header class="nav">
      <div class="nav-inner">
        <div class="brand" @click="$emit('go-home')">
          <div class="seal">弈</div>
          <div class="brand-txt">
            <b>太虚弈局</b>
            <span>TAI XU YI JU · 水墨棋枰</span>
          </div>
        </div>
        <nav class="crumbs">
          <a @click="$emit('go-home')">首页</a><span class="sep">›</span><span class="cur">太虚弈局</span>
        </nav>
        <div class="nav-right" v-if="bg && bg.asset">
          <span class="pill power">⚡ {{ bg.asset.power }}</span>
          <span class="pill copper">◉ {{ bg.asset.copper }}</span>
          <button class="back" @click="$emit('go-home')">‹ 返回</button>
        </div>
      </div>
    </header>

    <section class="loading" v-if="!bg">
      <div class="loading-mark">◇</div>
      <p>{{ loadMsg }}</p>
      <button class="retry" @click="load" v-if="loadFailed">重新探查</button>
    </section>

    <template v-if="bg">
      <!-- ===== 模式切换 ===== -->
      <div class="mode-tabs">
        <button :class="{ on: mode === 'solo' }" @click="mode = 'solo'">
          <span class="mt-ico">⚄</span><span>独弈 · 太虚棋局</span>
        </button>
        <button :class="{ on: mode === 'pvp' }" @click="switchPvp">
          <span class="mt-ico">⚔</span><span>擂台 · 论剑争锋</span>
        </button>
      </div>

      <!-- ============ 单人弈局 ============ -->
      <template v-if="mode === 'solo'">
        <section class="statusbar">
          <div class="sb-item"><span class="sb-ico">◈</span><div><b>{{ bg.daily_left }}</b><small>今日余局 / {{ bg.daily_limit }}</small></div></div>
          <div class="sb-item"><span class="sb-ico">◉</span><div><b>{{ bg.entry_fee }}</b><small>入局报名铜币</small></div></div>
          <div class="sb-item"><span class="sb-ico">♛</span><div><b>{{ bg.total_won }}</b><small>累计登顶 / {{ bg.total_played }} 局</small></div></div>
          <div class="sb-item"><span class="sb-ico">⚄</span><div><b>{{ bg.max_rolls }}</b><small>每局最多掷骰</small></div></div>
        </section>

        <main class="arena">
          <section class="board-wrap">
            <div class="board-head">
              <h2>太虚棋局 · 飞行棋</h2>
              <span>掷六起飞 · 同色飞跃 · 撞妖夺赏 · 四机登顶</span>
            </div>
            <div class="board">
              <div v-for="cell in bg.board" :key="cell.index" class="cell"
                   :class="['t-' + cell.type.toLowerCase(), 'color-' + cell.color,
                            { here: cur && myPlanes.includes(cell.index), finish: cell.type === 'FINISH' }]">
                <span class="cell-idx">{{ cell.index }}</span>
                <span class="cell-icon">{{ cell.icon }}</span>
                <span class="cell-name">{{ cell.name }}</span>
                <span class="cell-gain" v-if="cell.copper">◉{{ cell.copper }}</span>
                <span class="cell-gain pw" v-if="cell.power">⚡{{ cell.power }}</span>
                <span class="guard" v-if="cur && cur.guards && flight && flight.guard_cells.includes(cell.index) && cur.guards[flight.guard_cells.indexOf(cell.index)]"
                      title="劫妖守军：撞破得赏赐">☠</span>
                <template v-for="(p, pi) in myPlanes" :key="'p'+pi">
                  <span v-if="p === cell.index" class="plane"
                        :class="['plane-'+pi, { sel: selectedPlane === pi }]"
                        :style="{ '--pc': (planeMeta[pi] && planeMeta[pi].color) || '#fff' }">{{ pi + 1 }}</span>
                </template>
              </div>
            </div>
            <!-- 4 架飞机状态选择 -->
            <div class="plane-dock" v-if="cur && cur.status === 'playing'">
              <button v-for="(p, pi) in myPlanes" :key="'dock'+pi" class="plane-chip"
                      :class="['plane-'+pi, { sel: selectedPlane === pi, done: planeDone(p), hangar: p === HANGAR }]"
                      :style="{ '--pc': (planeMeta[pi] && planeMeta[pi].color) || '#fff' }"
                      :disabled="busy || planeDone(p)" @click="selectPlane(pi)">
                <span class="pc-ico">{{ planeMeta[pi] && planeMeta[pi].icon }}</span>
                <span class="pc-name">{{ planeMeta[pi] && planeMeta[pi].name }}</span>
                <span class="pc-pos">{{ planeLabel(p) }}</span>
              </button>
            </div>
            <div class="legend">
              <span><i class="dot c-copper"></i>铜币</span><span><i class="dot c-power"></i>神尊力</span>
              <span><i class="dot c-boon"></i>双得</span><span><i class="dot c-fortune"></i>福缘</span>
              <span><i class="dot c-item"></i>道具</span><span><i class="dot c-peril"></i>劫难</span>
              <span><i class="dot c-finish"></i>终点</span>
            </div>
          </section>

          <aside class="side">
            <div class="side-card play-card">
              <template v-if="cur && cur.status === 'playing'">
                <div class="play-head"><span class="live-dot"></span> 对局进行中</div>
                <div class="play-progress">
                  <div class="pp-bar"><i :style="{ width: Math.round(planeDoneCount / 4 * 100) + '%' }"></i></div>
                  <div class="pp-meta"><span>第 {{ cur.rolls }} / {{ bg.max_rolls }} 掷</span><span>登顶 {{ planeDoneCount }} / 4 机</span></div>
                </div>
                <p class="sel-tip">当前操作：<b :style="{ color: (planeMeta[selectedPlane] && planeMeta[selectedPlane].color) || '' }">{{ planeMeta[selectedPlane] && planeMeta[selectedPlane].icon }} {{ planeMeta[selectedPlane] && planeMeta[selectedPlane].name }}</b>（点上方飞机切换）</p>
                <div class="dice" :class="{ rolling: rolling }">{{ rolling ? '⚄' : diceFace }}</div>
                <button class="big-btn" :disabled="busy || rolling" @click="doRoll">{{ rolling ? '骰子转动中…' : '掷 骰 行 棋' }}</button>
                <p class="play-tip">本局已得 ◉{{ cur.gain_copper }} · ⚡{{ cur.gain_power }}<span v-if="cur.ward"> · 🛡护体</span></p>
              </template>
              <template v-else>
                <div class="play-head ready">太虚弈局 · 飞行棋</div>
                <p class="intro">四架飞机自机坪起飞：<b>掷出六点</b>方可起飞并再掷一次；落在<b>同色格</b>飞跃四步；撞破<b>劫妖</b>得赏赐 ◉{{ (flight && flight.guard_bonus) || 25 }}；点数超出终点原路反弹，须<b>精确</b>落在神位。四机全部登顶即通关，另得 <b>◉150</b> 与 <b>⚡100</b> 大奖。</p>
                <p class="cost-line">入局需 <b>◉ {{ bg.entry_fee }} 铜币</b> 报名</p>
                <button class="big-btn" :disabled="busy || bg.daily_left <= 0 || bg.asset.copper < bg.entry_fee" @click="doStart">
                  {{ bg.daily_left <= 0 ? '今日次数已尽' : (bg.asset.copper < bg.entry_fee ? '铜币不足' : '入 局 开 弈') }}
                </button>
                <p class="play-tip">每局最多 {{ bg.max_rolls }} 掷 · 每日 {{ bg.daily_limit }} 局</p>
              </template>
            </div>

            <!-- 道具背包 -->
            <div class="side-card item-card">
              <div class="item-head"><span>🎒 乾坤囊</span><small>落道具格可得 · 点击使用</small></div>
              <div class="item-grid">
                <button v-for="it in myItems" :key="it.key" class="item-chip" :class="{ empty: !it.cnt }"
                        :disabled="!it.cnt || busy" @click="doUseItem(it.key)" :title="it.desc">
                  <span class="ic">{{ it.icon }}</span>
                  <span class="nm">{{ it.name }}</span>
                  <span class="ct">×{{ it.cnt }}</span>
                </button>
              </div>
            </div>

            <transition name="pop">
              <div class="side-card result-card" v-if="lastRoll">
                <div class="rc-head">上一掷 · {{ lastRoll.plane_name || ('第' + ((lastRoll.plane || 0) + 1) + '机') }} {{ lastRoll.dice }} 点<template v-if="lastRoll.item_name"> · 得 {{ lastRoll.item_name }}</template><template v-if="lastRoll.ward_used"> · 🛡抵消劫难</template></div>
                <div class="rc-special" v-if="lastRoll.special">{{ lastRoll.special }}</div>
                <div class="rc-body">
                  <span class="rc-cell">{{ lastRoll.cell_icon }} {{ lastRoll.cell_name }}</span>
                  <span class="rc-gains">
                    <em v-if="lastRoll.gain_copper" :class="lastRoll.gain_copper < 0 ? 'minus' : ''">{{ lastRoll.gain_copper > 0 ? '+' : '' }}{{ lastRoll.gain_copper }} 铜币</em>
                    <em v-if="lastRoll.gain_power" :class="lastRoll.gain_power < 0 ? 'minus' : ''">{{ lastRoll.gain_power > 0 ? '+' : '' }}{{ lastRoll.gain_power }} 神尊力</em>
                    <em v-if="!lastRoll.gain_copper && !lastRoll.gain_power && !lastRoll.item_name">无事发生</em>
                  </span>
                </div>
              </div>
            </transition>
          </aside>
        </main>
      </template>

      <!-- ============ PVP 擂台 ============ -->
      <template v-else>
        <section class="statusbar pvp-status">
          <div class="sb-item"><span class="sb-ico">⚔</span><div><b>{{ pv.played || 0 }}</b><small>擂台场次 / {{ pv.wins || 0 }} 胜</small></div></div>
          <div class="sb-item"><span class="sb-ico">◉</span><div><b>{{ pv.entry_fee || 50 }}</b><small>报名铜币 / 方</small></div></div>
          <div class="sb-item"><span class="sb-ico">♛</span><div><b>{{ pv.win_copper || 100 }}</b><small>胜者赏赐铜币</small></div></div>
          <div class="sb-item"><span class="sb-ico">⚡</span><div><b>{{ pv.win_power || 50 }}</b><small>胜者赏赐神尊力</small></div></div>
        </section>

        <main class="arena">
          <section class="board-wrap">
            <div class="board-head">
              <h2>论剑棋枰 · 飞行棋</h2>
              <span>掷六起飞 · 同色飞跃 · 撞子回坪 · 四机登顶者胜</span>
            </div>
            <div class="board">
              <div v-for="cell in (bg.board || [])" :key="cell.index" class="cell"
                   :class="['t-' + cell.type.toLowerCase(), 'color-' + cell.color,
                            { finish: cell.type === 'FINISH',
                              here: pvMatch && ((isChallenger ? pvMatch.challenger_planes : pvMatch.defender_planes) || []).includes(cell.index) }]">
                <span class="cell-idx">{{ cell.index }}</span>
                <span class="cell-icon">{{ cell.icon }}</span>
                <span class="cell-name">{{ cell.name }}</span>
                <template v-for="(p, pi) in (pvMatch ? pvMatch.challenger_planes : [])" :key="'ca'+pi">
                  <span v-if="p === cell.index" class="plane plane-0" :class="{ enemy: !isChallenger, sel: isChallenger && selectedPlane === pi }"
                        style="--pc:#e9c46a">挑{{ pi + 1 }}</span>
                </template>
                <template v-for="(p, pi) in (pvMatch ? pvMatch.defender_planes : [])" :key="'de'+pi">
                  <span v-if="p === cell.index" class="plane plane-2" :class="{ enemy: isChallenger, sel: !isChallenger && selectedPlane === pi }"
                        style="--pc:#ef4454">应{{ pi + 1 }}</span>
                </template>
              </div>
            </div>
            <!-- PVP 我方飞机选择 -->
            <div class="plane-dock" v-if="pvMatch && pvMatch.status === 'playing'">
              <button v-for="(p, pi) in myPlanes" :key="'pv-dock'+pi" class="plane-chip"
                      :class="['plane-'+pi, { sel: selectedPlane === pi, done: planeDone(p), hangar: p === HANGAR }]"
                      :style="{ '--pc': (planeMeta[pi] && planeMeta[pi].color) || '#fff' }"
                      :disabled="pvpBusy || !myTurn || planeDone(p)" @click="selectPlane(pi)">
                <span class="pc-ico">{{ planeMeta[pi] && planeMeta[pi].icon }}</span>
                <span class="pc-name">{{ planeMeta[pi] && planeMeta[pi].name }}</span>
                <span class="pc-pos">{{ planeLabel(p) }}</span>
              </button>
            </div>
            <div class="legend"><span><i class="dot c-copper"></i>铜币</span><span><i class="dot c-power"></i>神尊力</span><span><i class="dot c-boon"></i>双得</span><span><i class="dot c-item"></i>道具</span><span><i class="dot c-peril"></i>劫难</span><span><i class="dot c-finish"></i>终点</span></div>
          </section>

          <aside class="side">
            <!-- 进行中的对局 -->
            <div class="side-card play-card" v-if="pvMatch && pvMatch.status === 'playing'">
              <div class="play-head"><span class="live-dot"></span> 第 #{{ pvMatch.id }} 场 · 对战中</div>
              <div class="duel-row">
                <div class="duel-side" :class="{ active: pvMatch.turn === 'challenger' }">
                  <span class="duel-name">{{ pvMatch.challenger_name }}</span>
                  <span class="duel-pos">登顶 {{ (pvMatch.challenger_planes || []).filter(p => p === ((pv.value.flight && pv.value.flight.finish_pos) || 15)).length }} / 4</span>
                  <span class="duel-roll">{{ pvMatch.challenger_rolls }} 掷</span>
                </div>
                <span class="duel-vs">VS</span>
                <div class="duel-side" :class="{ active: pvMatch.turn === 'defender' }">
                  <span class="duel-name">{{ pvMatch.defender_name }}</span>
                  <span class="duel-pos">登顶 {{ (pvMatch.defender_planes || []).filter(p => p === ((pv.value.flight && pv.value.flight.finish_pos) || 15)).length }} / 4</span>
                  <span class="duel-roll">{{ pvMatch.defender_rolls }} 掷</span>
                </div>
              </div>
              <div class="turn-tip">{{ myTurn ? '轮到你了（点选飞机后掷骰）' : (pvMatch.defender_ai ? '天道正在推演…' : '静待对手行棋') }}</div>
              <div class="dice small" :class="{ rolling: pvpRolling }">{{ pvpRolling ? '⚄' : pvpDice }}</div>
              <button class="big-btn" :disabled="pvpBusy || !myTurn" @click="doPvpRoll">{{ !myTurn ? '待对手行棋…' : '掷 骰 论 剑' }}</button>
              <p class="play-tip">奖池 ◉{{ pvMatch.pot }} · 胜者得 ◉{{ pv.entry_fee * 2 }} + 赏赐</p>
            </div>

            <!-- 创建挑战 -->
            <div class="side-card play-card" v-else>
              <div class="play-head ready">擂台论剑</div>
              <p class="intro">发起挑战需 <b>◉{{ pv.entry_fee }} 铜币</b>，天道或对手同注，奖池归胜者。飞行棋规则：掷六起飞、同色飞跃、撞子送回机坪，<b>四机全部登顶</b>者获胜。</p>
              <label class="field-label">挑战方式</label>
              <div class="pv-opts">
                <button :class="{ on: versusAi }" @click="versusAi = true">🤖 天道·无极</button>
                <button :class="{ on: !versusAi }" @click="versusAi = false">👤 指名道友</button>
              </div>
              <input v-if="!versusAi" class="name-input" v-model="opponentName" placeholder="输入对手神尊名号" />
              <button class="big-btn" :disabled="pvpBusy || bg.asset.copper < (pv.entry_fee || 50)" @click="doPvpCreate">
                {{ bg.asset.copper < (pv.entry_fee || 50) ? '铜币不足' : '发 起 挑 战' }}
              </button>
              <p class="play-tip">每日限 {{ pv.daily_limit || 5 }} 场 · 挑战后即刻入库</p>
            </div>

            <!-- 道具背包（PVP） -->
            <div class="side-card item-card">
              <div class="item-head"><span>🎒 乾坤囊</span><small>点击使用于当前对局</small></div>
              <div class="item-grid">
                <button v-for="it in myItems" :key="it.key" class="item-chip" :class="{ empty: !it.cnt }"
                        :disabled="!it.cnt || pvpBusy || !pvMatch || pvMatch.status !== 'playing'" @click="doPvpUseItem(it.key)" :title="it.desc">
                  <span class="ic">{{ it.icon }}</span><span class="nm">{{ it.name }}</span><span class="ct">×{{ it.cnt }}</span>
                </button>
              </div>
            </div>

            <!-- 可应战列表 -->
            <div class="side-card" v-if="challengeList.length">
              <div class="hc-head">可应战 · 悬赏榜</div>
              <ul class="chal-list">
                <li v-for="c in challengeList" :key="c.id">
                  <span class="chal-name">⚔ {{ c.challenger_name }}</span>
                  <span class="chal-pot">◉{{ c.pot }}</span>
                  <button @click="doPvpAccept(c.id)">应战</button>
                </li>
              </ul>
            </div>
          </aside>
        </main>

        <!-- PVP 战绩 -->
        <section class="extras pvp-extras">
          <div class="block">
            <div class="block-head"><span class="bh-icon">⚔</span><h2>我的战绩</h2><span class="bh-line"></span></div>
            <div class="pvp-record">
              <div><b>{{ pv.played || 0 }}</b><span>总场次</span></div>
              <div><b>{{ pv.wins || 0 }}</b><span>胜场</span></div>
            </div>
            <ul class="pvp-history" v-if="pvMatches.length">
              <li v-for="m in pvMatches.slice(0,6)" :key="m.id">
                <span>{{ m.challenger_name }} <em>vs</em> {{ m.defender_name }}</span>
                <span class="ph-res" :class="{ win: winnerIsMe(m) }">{{ m.status === 'finished' ? (winnerIsMe(m) ? '胜' : '负') : '进行中' }}</span>
              </li>
            </ul>
            <p class="rank-empty" v-else>尚无擂台战绩，发起挑战一试身手</p>
          </div>
        </section>
      </template>

      <!-- ===== 成就 + 排行榜 ===== -->
      <main class="extras" v-if="mode === 'solo'">
        <section class="block achieve-block">
          <div class="block-head"><span class="bh-icon">✧</span><h2>成就殿堂</h2><span class="bh-line"></span><span class="bh-tip">行棋问道，步步留痕</span></div>
          <div class="badge-grid">
            <div v-for="a in bg.achievements" :key="a.name" class="badge" :class="{ locked: !a.unlocked }">
              <span class="badge-mark">{{ a.unlocked ? '✦' : '◇' }}</span>
              <div class="badge-info"><b>{{ a.name }}</b><small>{{ a.desc }}</small></div>
              <span class="badge-state">{{ a.unlocked ? '已达成' : '未解锁' }}</span>
            </div>
          </div>
        </section>
        <section class="block rank-block">
          <div class="block-head"><span class="bh-icon">♛</span><h2>太虚榜</h2><span class="bh-line"></span><span class="bh-tip">群雄逐鹿，谁登神位</span></div>
          <div class="rank-tabs">
            <button :class="{ on: rankTab === 'wins' }" @click="rankTab = 'wins'">登顶榜</button>
            <button :class="{ on: rankTab === 'copper' }" @click="rankTab = 'copper'">财富榜</button>
            <button :class="{ on: rankTab === 'pvp' }" @click="rankTab = 'pvp'">擂台榜</button>
          </div>
          <div class="rank-list" v-if="rankRows.length">
            <div v-for="(r, i) in rankRows" :key="(r.name || '') + i" class="rank-row" :class="'rk-' + (i + 1)">
              <span class="rk-no">{{ i < 3 ? ['♛','♞','♜'][i] : (i + 1) }}</span>
              <span class="rk-name">{{ r.name }}</span>
              <span class="rk-stat" v-if="rankTab === 'wins'">{{ r.wins }} 次登顶</span>
              <span class="rk-stat" v-else-if="rankTab === 'pvp'">{{ r.wins || 0 }} 胜 / {{ r.plays || 0 }} 场</span>
              <span class="rk-stat" v-else>{{ r.copper }} 铜币</span>
              <span class="rk-gain">⚡{{ r.power != null ? r.power : '—' }} · ◉{{ r.copper != null ? r.copper : '—' }}</span>
            </div>
          </div>
          <p class="rank-empty" v-else>榜上无名，入局一较高下</p>
        </section>
      </main>

      <footer class="foot">沧元图 · 太虚弈局 —— 泼墨为枰 · 掷骰问道 · 落格得宝</footer>
    </template>

    <!-- 通关特效 -->
    <transition name="fade"><div class="win-flash" v-if="winFx"><div class="wf-card"><div class="wf-glow"></div><p class="wf-crown">♛</p><p class="wf-title">{{ winFx.title || '登 临 神 位' }}</p><p class="wf-sub">{{ winFx.sub || '本局收获' }}</p><p class="wf-gain">◉ {{ winFx.gain_copper }} 铜币　⚡ {{ winFx.gain_power }} 神尊力</p></div></div></transition>

    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import {
  fetchBoardGame, startBoardGame, rollBoardGame, fetchBoardGameRank,
  fetchBoardItems, useBoardItem, fetchPvpStatus, fetchPvpList, createPvp, acceptPvp, rollPvp
} from '../api'

const props = defineProps({ avatar: { type: Object, default: null } })
defineEmits(['go-home'])

const motes = Array.from({ length: 16 }, () => {
  const size = 2 + Math.random() * 4
  return {
    left: (Math.random() * 100).toFixed(2) + '%',
    width: size + 'px', height: size + 'px',
    animation: `drift linear infinite ${(12 + Math.random() * 12).toFixed(2)}s ${(-Math.random() * 14).toFixed(2)}s`
  }
})

const bg = ref(null)
const busy = ref(false)
const rolling = ref(false)
const diceFace = ref('⚄')
const loadFailed = ref(false)
const loadMsg = ref('正在铺开棋枰…')
const lastRoll = ref(null)
const winFx = ref(null)
const rank = ref(null)
const rankTab = ref('wins')
const mode = ref('solo')

// PVP 状态
const pv = ref({})               // pvp/status 返回内容
const pvMatches = ref([])
const challengeList = ref([])
const pvMatch = computed(() => pv.value && pv.value.current)
const isChallenger = computed(() => {
  const a = props.avatar || {}
  return !!(pvMatch.value && pvMatch.value.challenger_name === a.deity_name)
})
const myTurn = computed(() => {
  const m = pvMatch.value
  if (!m) return false
  return isChallenger.value ? m.turn === 'challenger' : m.turn === 'defender'
})
const pvpBusy = ref(false)
const pvpRolling = ref(false)
const pvpDice = ref('⚄')
const versusAi = ref(true)
const opponentName = ref('')

const cur = computed(() => bg.value && bg.value.current)
const myItems = computed(() => (bg.value && bg.value.items) || [])

/* ===== 飞行棋：4 架飞机 ===== */
const flight = computed(() => (bg.value && bg.value.flight) || (pv.value && pv.value.flight) || null)
const planeMeta = computed(() => (flight.value && flight.value.plane_meta) || [])
const HANGAR = -1
const FINISH_POS = computed(() => (flight.value && flight.value.finish_pos) || ((bg.value?.board_size || 16) - 1))
function planeAt(pos, planeIdx) {
  return pos >= 0 && pos === planeIdx ? true : false
}
/** 当前对局飞机位置（单人 / PVP 我方） */
const myPlanes = computed(() => {
  if (mode.value === 'pvp') {
    return (pvMatch.value && (isChallenger.value
      ? pvMatch.value.challenger_planes : pvMatch.value.defender_planes)) || []
  }
  return (cur.value && cur.value.planes) || []
})
/** 对方飞机位置（PVP） */
const foePlanes = computed(() => {
  if (mode.value === 'pvp' && pvMatch.value) {
    return isChallenger.value ? pvMatch.value.defender_planes : pvMatch.value.challenger_planes
  }
  return []
})
/** 当前选中的飞机（本地即时选中，后端也会记住） */
const selectedPlane = ref(0)
watch(cur, (g) => { if (g && g.active_plane != null) selectedPlane.value = g.active_plane })
watch(pvMatch, (m) => {
  if (m) selectedPlane.value = isChallenger.value
    ? (m.challenger_active || 0) : (m.defender_active || 0)
})
function selectPlane(i) {
  if (busy.value || pvpBusy.value) return
  selectedPlane.value = i
}
function planeDone(pos) { return pos === FINISH_POS.value }
function planeLabel(pos) { return pos === HANGAR ? '机坪' : (planeDone(pos) ? '登顶' : ('格' + pos)) }
const planeDoneCount = computed(() => myPlanes.value.filter(planeDone).length)
const rankRows = computed(() => {
  if (!rank.value) return []
  if (rankTab.value === 'wins') return rank.value.by_wins || []
  if (rankTab.value === 'pvp') return rank.value.by_pvp || []
  return (rank.value.by_copper || []).map(r => ({ ...r, wins: '—' }))
})

function cred() { const a = props.avatar || {}; return { name: a.deity_name, spell: a.spell } }

async function load() {
  const a = props.avatar || {}
  if (!a.deity_name) { loadMsg.value = '游客无根基，请先觉醒神尊'; loadFailed.value = true; return }
  loadFailed.value = false
  loadMsg.value = '正在铺开棋枰…'
  bg.value = null
  const d = await fetchBoardGame(a.deity_name)
  if (d && d.ok && d.boardgame) bg.value = d.boardgame
  else { loadFailed.value = true; loadMsg.value = (d && d.msg) || '无法连接元界（后端未启动？）'; return }
  const r = await fetchBoardGameRank()
  if (r && r.ok) rank.value = r
  await loadPvp(false)
}
onMounted(load)

async function loadPvp(loadList = true) {
  const name = (props.avatar || {}).deity_name
  if (!name) return
  const s = await fetchPvpStatus(name)
  if (s && s.ok) {
    pv.value = s
    pvMatches.value = s.matches || []
    if (bg.value) { bg.value.asset = s.asset; bg.value.items = s.items }
  }
  if (loadList) {
    const l = await fetchPvpList(name)
    if (l && l.ok) challengeList.value = l.list || []
  }
}
async function switchPvp() { mode.value = 'pvp'; await loadPvp(true) }

async function refreshAll() {
  const name = (props.avatar || {}).deity_name
  const s = await fetchBoardGame(name)
  if (s && s.ok && s.boardgame) bg.value = s.boardgame
  await loadPvp(true)
}

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
    await refreshAll()
  } else toast(d.msg || '入局失败')
}

async function doRoll() {
  if (busy.value || rolling.value) return
  busy.value = true; rolling.value = true
  const faces = ['⚀','⚁','⚂','⚃','⚄','⚅']
  let ticks = 0
  const timer = setInterval(() => { diceFace.value = faces[Math.floor(Math.random()*6)]; if (++ticks > 14) clearInterval(timer) }, 70)
  const d = await rollBoardGame(cred(), selectedPlane.value)
  await new Promise(r => setTimeout(r, 850))
  clearInterval(timer); rolling.value = false; busy.value = false
  if (d.ok) {
    diceFace.value = faces[(d.dice || 1) - 1]
    lastRoll.value = d
    await refreshAll()
    if (d.pass === 1) {
      winFx.value = { title: '登 临 神 位', sub: '本局收获', gain_copper: d.game.gain_copper, gain_power: d.game.gain_power }
      setTimeout(() => { winFx.value = null }, 2800)
    }
    const g = d.gain_copper ? `${d.gain_copper > 0 ? '+' : ''}${d.gain_copper} 铜币 ` : ''
    const p = d.gain_power ? `${d.gain_power > 0 ? '+' : ''}${d.gain_power} 神尊力` : ''
    const item = d.item_name ? ` 得【${d.item_name}】` : ''
    const sp = d.special ? ` · ${d.special}` : ''
    toast(d.finished ? (d.pass === 1 ? '四机登顶，登临神位！' : '掷骰已尽') : `掷出 ${d.dice} 点，落「${d.cell_name}」${g}${p}${item}${sp}`)
  } else toast(d.msg || '掷骰失败')
}

async function doUseItem(key) {
  if (busy.value) return
  busy.value = true
  const d = await useBoardItem({ ...cred(), item_key: key })
  busy.value = false
  if (d.ok) { toast(d.msg); await refreshAll() }
  else toast(d.msg || '使用失败')
}

/* ===== PVP 操作 ===== */
async function doPvpCreate() {
  if (pvpBusy.value) return
  pvpBusy.value = true
  const payload = { ...cred(), versus_ai: versusAi.value }
  if (!versusAi.value) {
    if (!opponentName.value.trim()) { toast('请输入对手名号'); pvpBusy.value = false; return }
    payload.opponent_name = opponentName.value.trim()
  }
  const d = await createPvp(payload)
  pvpBusy.value = false
  if (d.ok) { toast(d.msg); await loadPvp(true) }
  else toast(d.msg || '发起失败')
}
async function doPvpAccept(id) {
  if (pvpBusy.value) return
  pvpBusy.value = true
  const d = await acceptPvp({ ...cred(), match_id: id })
  pvpBusy.value = false
  if (d.ok) { toast(d.msg); await loadPvp(true) }
  else toast(d.msg || '应战失败')
}
async function doPvpRoll() {
  if (pvpBusy.value || pvpRolling.value || !pvMatch.value) return
  pvpBusy.value = true; pvpRolling.value = true
  const faces = ['⚀','⚁','⚂','⚃','⚄','⚅']
  let ticks = 0
  const timer = setInterval(() => { pvpDice.value = faces[Math.floor(Math.random()*6)]; if (++ticks > 14) clearInterval(timer) }, 70)
  const d = await rollPvp({ ...cred(), match_id: pvMatch.value.id, plane_index: selectedPlane.value })
  await new Promise(r => setTimeout(r, 850))
  clearInterval(timer); pvpRolling.value = false; pvpBusy.value = false
  if (d.ok) {
    await loadPvp(true)
    toast(d.msg)
    if (d.finished) {
      winFx.value = {
        title: d.win ? '擂 台 得 胜' : '惜 败 擂 台',
        sub: d.win ? '奖池与赏赐已入囊' : '来日再战',
        gain_copper: d.win ? (pv.value.win_copper || 0) + (pv.value.entry_fee || 0) : 0,
        gain_power: d.win ? (pv.value.win_power || 0) : 0
      }
      setTimeout(() => { winFx.value = null }, 2800)
    }
  } else toast(d.msg || '掷骰失败')
}
async function doPvpUseItem(key) {
  if (pvpBusy.value || !pvMatch.value) return
  pvpBusy.value = true
  const d = await useBoardItem({ ...cred(), item_key: key, match_id: pvMatch.value.id })
  pvpBusy.value = false
  if (d.ok) { toast(d.msg); await loadPvp(true) }
  else toast(d.msg || '使用失败')
}
function winnerIsMe(m) {
  const a = props.avatar || {}
  const myName = a.deity_name
  return (m.winner === 'challenger' && m.challenger_name === myName)
      || (m.winner === 'defender' && m.defender_name === myName)
}

const toastMsg = ref(''); const toastShow = ref(false); let toastTimer = null
function toast(msg) {
  toastMsg.value = msg; toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 3000)
}
</script>

<style scoped>
/* ========== 水墨风基调 ========== */
.boardgame{position:fixed;inset:0;overflow-y:auto;overflow-x:hidden;
  background:
    radial-gradient(90% 60% at 12% 4%,rgba(45,212,191,.10),transparent 60%),
    radial-gradient(80% 70% at 92% 96%,rgba(45,212,191,.07),transparent 60%),
    repeating-linear-gradient(0deg,rgba(255,255,255,.012) 0,rgba(255,255,255,.012) 1px,transparent 1px,transparent 4px),
    linear-gradient(180deg,#0d1426,#070b16 72%);
  color:var(--text)}
.boardgame::before{content:"";position:fixed;inset:0;pointer-events:none;z-index:0;opacity:.5;
  background:radial-gradient(60% 40% at 80% 12%,rgba(255,255,255,.05),transparent 70%),radial-gradient(50% 40% at 10% 80%,rgba(255,255,255,.04),transparent 70%)}
.ink-drift{position:fixed;inset:0;pointer-events:none;z-index:0;overflow:hidden}
.ink-drift i{position:absolute;bottom:-10px;border-radius:50%;background:rgba(45,212,191,.5);opacity:0}
@keyframes drift{0%{transform:translateY(0) translateX(0);opacity:0}10%{opacity:.5}50%{transform:translateY(-55vh) translateX(30px)}90%{opacity:.25}100%{transform:translateY(-108vh) translateX(-20px);opacity:0}}

.nav{position:sticky;top:0;z-index:30;background:linear-gradient(180deg,rgba(7,11,22,.94),rgba(7,11,22,.66));
  backdrop-filter:blur(14px);border-bottom:1px solid rgba(255,255,255,.14);box-shadow:0 8px 30px rgba(0,0,0,.4)}
.nav-inner{max-width:1280px;margin:0 auto;display:flex;align-items:center;gap:26px;padding:14px 26px}
.brand{display:flex;align-items:center;gap:12px;cursor:pointer;flex-shrink:0}
.seal{width:42px;height:42px;border-radius:8px;background:linear-gradient(135deg,#ef4454,#b91c1c);
  display:grid;place-items:center;font-weight:900;color:#e8eef7;font-size:20px;
  border:1px solid rgba(232,238,247,.35);box-shadow:0 0 18px rgba(239,68,84,.4);font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.brand-txt b{font-size:18px;letter-spacing:4px;font-weight:900;display:block;color:#e8eef7;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.brand-txt span{font-size:10px;color:#9fb0c8;letter-spacing:2px}
.crumbs{display:flex;align-items:center;gap:10px;flex:1;font-size:13px;letter-spacing:1px;color:#9fb0c8}
.crumbs a{cursor:pointer;transition:.15s}.crumbs a:hover{color:#e9c46a}.crumbs .sep{color:#3a465c}.crumbs .cur{color:#e9c46a}
.nav-right{display:flex;align-items:center;gap:10px;flex-shrink:0}
.pill{padding:6px 14px;border-radius:999px;font-size:13px;letter-spacing:1px;font-weight:700;border:1px solid rgba(255,255,255,.16)}
.pill.power{color:#2dd4bf;background:rgba(45,212,191,.10)}
.pill.copper{color:#e9c46a;background:rgba(233,196,106,.10);border-color:rgba(233,196,106,.35)}
.back{padding:6px 14px;border:1px solid rgba(255,255,255,.18);border-radius:999px;background:none;color:#9fb0c8;cursor:pointer;font-family:inherit;font-size:12px;letter-spacing:2px;transition:.15s}
.back:hover{color:#e9c46a;border-color:#e9c46a}

.loading{text-align:center;padding:120px 26px;color:#9fb0c8;position:relative;z-index:2}
.loading-mark{font-size:54px;color:#3a465c;margin-bottom:14px}.loading p{letter-spacing:2px;line-height:2}
.retry{margin-top:18px;padding:8px 22px;border:1px solid rgba(255,255,255,.2);border-radius:999px;background:none;color:#e9c46a;cursor:pointer;font-family:inherit;font-size:13px;letter-spacing:2px}

/* 模式切换 */
.mode-tabs{max-width:1280px;margin:22px auto 0;padding:0 26px;display:flex;gap:12px;position:relative;z-index:2}
.mode-tabs button{flex:1;display:flex;align-items:center;justify-content:center;gap:10px;padding:13px;
  border:1px solid rgba(255,255,255,.16);border-radius:12px;background:rgba(13,20,38,.6);color:#9fb0c8;
  font-family:inherit;font-size:15px;letter-spacing:3px;cursor:pointer;transition:.2s}
.mode-tabs button:hover{border-color:rgba(233,196,106,.5);color:#e9c46a}
.mode-tabs button.on{background:linear-gradient(135deg,rgba(45,212,191,.22),rgba(13,20,38,.6));border-color:#2dd4bf;color:#e8eef7;font-weight:800;box-shadow:inset 0 0 24px rgba(45,212,191,.15)}
.mt-ico{font-size:19px}

.statusbar{max-width:1280px;margin:18px auto 0;padding:0 26px;display:grid;grid-template-columns:repeat(4,1fr);gap:14px;position:relative;z-index:2}
.sb-item{display:flex;align-items:center;gap:14px;padding:15px 20px;border-radius:12px;background:rgba(13,20,38,.55);border:1px solid rgba(255,255,255,.12)}
.sb-ico{font-size:23px;color:#2dd4bf;text-shadow:0 0 12px rgba(45,212,191,.5)}
.sb-item b{font-size:21px;font-weight:900;color:#e9c46a;display:block;line-height:1.2;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.sb-item small{font-size:11px;color:#9fb0c8;letter-spacing:1px}

.arena{max-width:1280px;margin:18px auto 0;padding:0 26px 30px;display:grid;grid-template-columns:1fr 340px;gap:20px;position:relative;z-index:2}
.board-wrap{padding:24px;border-radius:14px;background:linear-gradient(160deg,rgba(255,255,255,.055),rgba(13,20,38,.7));
  border:1px solid rgba(255,255,255,.14);box-shadow:0 22px 60px rgba(0,0,0,.4)}
.board-head{display:flex;align-items:baseline;gap:16px;margin-bottom:18px}
.board-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:25px;letter-spacing:5px;font-weight:900;color:#e8eef7}
.board-head span{font-size:12px;color:#9fb0c8;letter-spacing:2px}

.board{display:grid;grid-template-columns:repeat(8,1fr);gap:9px}
.cell{position:relative;min-height:102px;border-radius:8px;padding:9px 5px;display:flex;flex-direction:column;align-items:center;
  justify-content:center;gap:3px;background:rgba(13,20,38,.85);border:1px solid rgba(255,255,255,.12);transition:.25s;overflow:hidden}
.cell::after{content:"";position:absolute;inset:0;opacity:.07;pointer-events:none}
.cell.t-copper::after{background:#e9c46a}.cell.t-power::after{background:#2dd4bf}
.cell.t-boon::after{background:linear-gradient(135deg,#2dd4bf,#e9c46a)}
.cell.t-fortune::after{background:#5eead4}.cell.t-item::after{background:#e9c46a}
.cell.t-peril::after{background:#ef4454}
.cell.t-finish{background:linear-gradient(135deg,rgba(233,196,106,.20),rgba(13,20,38,.85));border-color:rgba(233,196,106,.55)}
.cell.t-start{background:rgba(45,212,191,.10)}
.cell:not(.here):hover{transform:translateY(-2px);border-color:rgba(255,255,255,.3)}
.cell.here{transform:translateY(-6px) scale(1.03);border-color:#e9c46a;box-shadow:0 0 0 2px #e9c46a,0 0 28px rgba(233,196,106,.35),0 14px 34px rgba(0,0,0,.4);z-index:3}
.cell-idx{position:absolute;top:5px;left:7px;font-size:10px;color:#9fb0c8;opacity:.7}
.cell-icon{font-size:24px;line-height:1;text-shadow:0 0 10px currentColor}
.t-copper .cell-icon,.t-finish .cell-icon{color:#e9c46a}.t-power .cell-icon,.t-start .cell-icon{color:#2dd4bf}
.t-boon .cell-icon{color:#5eead4}.t-fortune .cell-icon{color:#5eead4}.t-item .cell-icon{color:#e9c46a}.t-peril .cell-icon{color:#ff7a85}
.cell-name{font-size:12px;letter-spacing:1px;color:#e8eef7;font-weight:700;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.cell-gain{font-size:11px;color:#e9c46a}.cell-gain.pw{color:#2dd4bf}
.pawn{position:absolute;top:-8px;right:-6px;width:24px;height:24px;border-radius:50%;background:linear-gradient(135deg,#2dd4bf,#0f766e);
  color:#04130f;font-size:11px;font-weight:900;display:grid;place-items:center;box-shadow:0 0 14px rgba(45,212,191,.7);animation:pawnPulse 1.3s ease-in-out infinite;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.pawn-a{background:linear-gradient(135deg,#e9c46a,#b8862b)}.pawn-a.mine{box-shadow:0 0 0 2px #e8eef7,0 0 16px rgba(233,196,106,.8)}
.pawn-b{background:linear-gradient(135deg,#2dd4bf,#0f766e);color:#04130f;box-shadow:0 0 14px rgba(45,212,191,.6)}.pawn-b.mine{box-shadow:0 0 0 2px #e8eef7,0 0 16px rgba(45,212,191,.8)}
@keyframes pawnPulse{50%{transform:scale(1.15)}}

/* ===== 飞行棋：格子颜色 / 飞机棋子 / 选机坞 ===== */
.cell.color-0{box-shadow:inset 3px 3px 0 rgba(45,212,191,.35)}
.cell.color-1{box-shadow:inset 3px 3px 0 rgba(233,196,106,.35)}
.cell.color-2{box-shadow:inset 3px 3px 0 rgba(239,68,84,.35)}
.cell.color-3{box-shadow:inset 3px 3px 0 rgba(232,238,247,.30)}
.cell.here.color-0{box-shadow:0 0 0 2px #2dd4bf,0 0 28px rgba(45,212,191,.35)}
.cell.here.color-1{box-shadow:0 0 0 2px #e9c46a,0 0 28px rgba(233,196,106,.35)}
.cell.here.color-2{box-shadow:0 0 0 2px #ef4454,0 0 28px rgba(239,68,84,.35)}
.cell.here.color-3{box-shadow:0 0 0 2px #e8eef7,0 0 28px rgba(232,238,247,.3)}
.guard{position:absolute;top:4px;right:6px;font-size:14px;color:#ef4454;text-shadow:0 0 10px rgba(239,68,84,.9);animation:pawnPulse 1.6s ease-in-out infinite}
.plane{position:absolute;bottom:3px;right:3px;min-width:20px;height:20px;padding:0 4px;border-radius:6px;
  background:var(--pc);color:#04130f;font-size:10px;font-weight:900;display:grid;place-items:center;
  box-shadow:0 0 10px var(--pc);border:1px solid rgba(255,255,255,.6);z-index:4;font-family:inherit}
.plane.enemy{border-color:#04130f;opacity:.92}
.plane.sel{outline:2px solid #fff;outline-offset:1px}
.plane-dock{display:flex;flex-wrap:wrap;gap:8px;margin-top:14px;justify-content:center}
.plane-chip{display:flex;align-items:center;gap:6px;padding:6px 11px;border-radius:999px;cursor:pointer;
  background:rgba(7,11,22,.7);border:1px solid rgba(255,255,255,.16);color:#e8eef7;font-family:inherit;
  font-size:11px;letter-spacing:.5px;transition:.16s}
.plane-chip .pc-ico{color:var(--pc);font-size:14px}
.plane-chip .pc-name{font-weight:700}
.plane-chip .pc-pos{color:#9fb0c8}
.plane-chip.sel{border-color:var(--pc);background:rgba(255,255,255,.08);box-shadow:0 0 16px var(--pc)}
.plane-chip.done{opacity:.5;cursor:default;border-color:#2dd4bf}
.plane-chip.hangar .pc-pos{color:#ef4454}
.plane-chip:disabled{opacity:.45;cursor:not-allowed}
.sel-tip{font-size:12px;color:#9fb0c8;letter-spacing:1px;margin:2px 0 8px}
.sel-tip b{font-weight:800}
.rc-special{font-size:12px;color:#e9c46a;letter-spacing:.5px;margin-top:4px}

.legend{display:flex;flex-wrap:wrap;gap:15px;margin-top:16px;padding-top:13px;border-top:1px dashed rgba(255,255,255,.16)}
.legend span{display:flex;align-items:center;gap:6px;font-size:11px;color:#9fb0c8;letter-spacing:1px}
.dot{width:9px;height:9px;border-radius:50%;display:inline-block}
.c-copper{background:#e9c46a}.c-power{background:#2dd4bf}.c-boon{background:linear-gradient(135deg,#2dd4bf,#e9c46a)}
.c-fortune{background:#5eead4}.c-item{background:#e9c46a}.c-peril{background:#ef4454}.c-finish{background:#e8eef7}

.side{display:flex;flex-direction:column;gap:14px}
.side-card{padding:19px;border-radius:12px;background:rgba(13,20,38,.6);border:1px solid rgba(255,255,255,.12)}
.play-card{text-align:center}
.play-head{font-size:14px;letter-spacing:3px;font-weight:800;color:#2dd4bf;margin-bottom:13px;display:flex;align-items:center;justify-content:center;gap:8px}
.play-head.ready{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:21px;color:#e8eef7}
.live-dot{width:8px;height:8px;border-radius:50%;background:#2dd4bf;box-shadow:0 0 10px #2dd4bf;animation:pawnPulse 1.3s infinite}
.play-progress{margin-bottom:12px}.pp-bar{height:8px;border-radius:999px;background:rgba(0,0,0,.5);border:1px solid rgba(255,255,255,.14);overflow:hidden}
.pp-bar i{display:block;height:100%;border-radius:999px;background:linear-gradient(90deg,#0f766e,#2dd4bf,#e9c46a);transition:width .5s}
.pp-meta{display:flex;justify-content:space-between;margin-top:6px;font-size:11px;color:#9fb0c8;letter-spacing:1px}
.dice{font-size:66px;line-height:1.1;color:#e8eef7;text-shadow:0 0 24px rgba(255,255,255,.3);margin:4px 0;filter:drop-shadow(0 6px 10px rgba(0,0,0,.5))}
.dice.small{font-size:48px}.dice.rolling{animation:shake .2s linear infinite}
@keyframes shake{0%{transform:rotate(-12deg)}50%{transform:rotate(12deg)}100%{transform:rotate(-12deg)}}
.big-btn{width:100%;margin-top:10px;padding:12px;border:none;border-radius:10px;cursor:pointer;font-family:inherit;
  font-size:15px;font-weight:900;letter-spacing:5px;color:#04130f;background:linear-gradient(135deg,#2dd4bf,#5eead4);
  box-shadow:0 10px 26px rgba(45,212,191,.28);transition:.18s}
.big-btn:hover:not(:disabled){filter:brightness(1.12);transform:translateY(-1px)}
.big-btn:disabled{opacity:.4;cursor:not-allowed;box-shadow:none}
.intro{font-size:13px;line-height:1.9;color:#9fb0c8;letter-spacing:.5px;text-align:left;margin:6px 0 12px}
.intro b{color:#e9c46a}.cost-line{font-size:13px;color:#c7d4e8;letter-spacing:1px;margin-bottom:6px}.cost-line b{color:#e9c46a}
.play-tip{font-size:11px;color:#9fb0c8;letter-spacing:1px;margin-top:11px}

/* 道具栏 */
.item-head{display:flex;justify-content:space-between;align-items:baseline;margin-bottom:12px}
.item-head span{font-size:14px;color:#e8eef7;letter-spacing:2px;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.item-head small{font-size:10px;color:#9fb0c8;letter-spacing:1px}
.item-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
.item-chip{position:relative;display:flex;flex-direction:column;align-items:center;gap:3px;padding:10px 3px;border-radius:9px;
  border:1px solid rgba(233,196,106,.35);background:rgba(13,20,38,.7);color:#c7d4e8;cursor:pointer;transition:.18s;font-family:inherit}
.item-chip:hover:not(:disabled){transform:translateY(-3px);border-color:#e9c46a;background:rgba(233,196,106,.10)}
.item-chip:disabled{cursor:not-allowed}
.item-chip.empty{opacity:.35;border-color:rgba(255,255,255,.12)}
.item-chip .ic{font-size:21px}.item-chip .nm{font-size:10px;letter-spacing:1px;white-space:nowrap}
.item-chip .ct{position:absolute;top:-6px;right:-5px;min-width:18px;height:18px;padding:0 4px;border-radius:9px;background:#2dd4bf;color:#04130f;font-size:10px;line-height:18px;font-weight:800}

.rc-head{font-size:11px;color:#2dd4bf;letter-spacing:1px;margin-bottom:9px}.rc-body{display:flex;flex-direction:column;gap:6px}
.rc-cell{font-size:14px;font-weight:700;letter-spacing:1px;color:#e8eef7;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.rc-gains{display:flex;flex-wrap:wrap;gap:9px}.rc-gains em{font-style:normal;font-size:12px;color:#e9c46a}.rc-gains em.minus{color:#ff7a85}
.result-card{border-color:rgba(45,212,191,.3)}
.hc-head{font-size:13px;color:#e9c46a;letter-spacing:2px;margin-bottom:10px;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}

/* PVP 对战 */
.duel-row{display:flex;align-items:center;justify-content:space-between;gap:8px;margin:6px 0 14px}
.duel-side{flex:1;display:flex;flex-direction:column;gap:3px;padding:10px 6px;border-radius:9px;background:rgba(13,20,38,.7);border:1px solid rgba(255,255,255,.12);transition:.2s}
.duel-side.active{border-color:#2dd4bf;box-shadow:inset 0 0 18px rgba(45,212,191,.18)}
.duel-name{font-size:12px;color:#e8eef7;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}
.duel-pos{font-size:18px;color:#e9c46a;font-weight:900}.duel-roll{font-size:10px;color:#9fb0c8}
.duel-vs{font-size:13px;color:#9fb0c8;font-weight:900;letter-spacing:1px}
.turn-tip{font-size:11px;color:#2dd4bf;letter-spacing:2px;margin-bottom:4px}
.field-label{display:block;text-align:left;font-size:11px;color:#9fb0c8;letter-spacing:2px;margin:12px 0 6px}
.pv-opts{display:flex;gap:8px}.pv-opts button{flex:1;padding:9px 4px;border:1px solid rgba(255,255,255,.16);border-radius:9px;background:rgba(13,20,38,.7);
  color:#9fb0c8;font-family:inherit;font-size:12px;letter-spacing:1px;cursor:pointer;transition:.18s}
.pv-opts button.on{border-color:#2dd4bf;color:#2dd4bf;background:rgba(45,212,191,.14)}
.name-input{width:100%;margin-top:10px;padding:10px 12px;border:1px solid rgba(255,255,255,.18);border-radius:9px;background:rgba(13,20,38,.8);
  color:#e8eef7;font-family:inherit;font-size:13px;letter-spacing:1px;box-sizing:border-box}
.name-input:focus{outline:none;border-color:#e9c46a}
.chal-list{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:7px}
.chal-list li{display:flex;align-items:center;gap:8px;font-size:12px;color:#c7d4e8}
.chal-name{flex:1;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.chal-pot{color:#e9c46a}.chal-list button{padding:4px 12px;border:1px solid #2dd4bf;border-radius:7px;background:rgba(45,212,191,.16);color:#2dd4bf;
  font-family:inherit;font-size:11px;letter-spacing:1px;cursor:pointer}.chal-list button:hover{background:#2dd4bf;color:#04130f}

.pvp-record{display:flex;gap:12px;margin-bottom:14px}
.pvp-record div{flex:1;display:flex;flex-direction:column;align-items:center;padding:12px;border-radius:10px;background:rgba(13,20,38,.7);border:1px solid rgba(255,255,255,.1)}
.pvp-record b{font-size:26px;color:#e9c46a;font-family:"ZCOOL XiaoWei","Noto Serif SC",serif}.pvp-record span{font-size:11px;color:#9fb0c8;letter-spacing:1px}
.pvp-history{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:7px}
.pvp-history li{display:flex;justify-content:space-between;gap:8px;font-size:12px;color:#9fb0c8;padding:7px 10px;border-radius:8px;background:rgba(13,20,38,.55)}
.pvp-history em{font-style:normal;color:#6b7a92;margin:0 3px}.ph-res{color:#9fb0c8}.ph-res.win{color:#e9c46a;font-weight:800}

/* 成就 + 排行榜 */
.extras{max-width:1280px;margin:2px auto 0;padding:0 26px 30px;display:grid;grid-template-columns:1fr 1fr;gap:20px;position:relative;z-index:2}
.pvp-extras{grid-template-columns:1fr}
.block{padding:23px;border-radius:14px;background:linear-gradient(160deg,rgba(255,255,255,.05),rgba(13,20,38,.7));border:1px solid rgba(255,255,255,.13)}
.block-head{display:flex;align-items:baseline;gap:13px;margin-bottom:18px}
.bh-icon{font-size:19px;color:#2dd4bf;text-shadow:0 0 10px rgba(45,212,191,.4)}
.block-head h2{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:21px;letter-spacing:4px;font-weight:900;color:#e8eef7}
.bh-line{flex:1;height:1px;background:linear-gradient(90deg,rgba(255,255,255,.2),transparent)}
.bh-tip{font-size:11px;color:#9fb0c8;letter-spacing:1px}
.badge-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:11px}
.badge{display:flex;align-items:center;gap:11px;padding:13px;border-radius:10px;background:rgba(13,20,38,.6);border:1px solid rgba(233,196,106,.35);transition:.2s}
.badge:hover{transform:translateY(-3px);border-color:#e9c46a}.badge.locked{opacity:.4;border-color:rgba(255,255,255,.12)}
.badge-mark{font-size:24px;color:#e9c46a;text-shadow:0 0 12px rgba(233,196,106,.4)}.badge.locked .badge-mark{color:#6b7a92;text-shadow:none}
.badge-info{flex:1;min-width:0}.badge-info b{display:block;font-size:13px;letter-spacing:1px;color:#e8eef7}
.badge-info small{display:block;font-size:10px;color:#9fb0c8;margin-top:3px}
.badge-state{font-size:10px;color:#e9c46a;letter-spacing:1px;white-space:nowrap}.badge.locked .badge-state{color:#9fb0c8}
.rank-tabs{display:flex;gap:7px;margin-bottom:13px}
.rank-tabs button{flex:1;padding:8px;border:1px solid rgba(255,255,255,.16);border-radius:9px;background:rgba(13,20,38,.6);color:#9fb0c8;font-family:inherit;font-size:12px;letter-spacing:2px;cursor:pointer;transition:.15s}
.rank-tabs button.on{color:#04130f;background:linear-gradient(135deg,#2dd4bf,#5eead4);border-color:transparent;font-weight:700}
.rank-list{display:flex;flex-direction:column;gap:6px}
.rank-row{display:flex;align-items:center;gap:9px;padding:9px 12px;border-radius:8px;background:rgba(13,20,38,.5);border:1px solid rgba(255,255,255,.06);font-size:12px;transition:.15s}
.rank-row:hover{border-color:#2dd4bf;transform:translateX(3px)}
.rank-row.rk-1{background:linear-gradient(90deg,rgba(233,196,106,.16),rgba(13,20,38,.5));border-color:rgba(233,196,106,.45)}
.rk-no{width:24px;text-align:center;font-size:15px;color:#e9c46a;font-weight:900}
.rk-name{flex:1;color:#e8eef7;letter-spacing:1px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.rk-stat{color:#2dd4bf;white-space:nowrap}.rk-gain{color:#9fb0c8;font-size:10px;white-space:nowrap}
.rank-empty{font-size:12px;color:#9fb0c8;letter-spacing:1px;text-align:center;padding:20px 0}

.foot{position:relative;z-index:2;text-align:center;padding:22px;font-size:11px;color:#6b7a92;letter-spacing:2px;border-top:1px solid rgba(255,255,255,.08)}

.win-flash{position:fixed;inset:0;z-index:90;display:grid;place-items:center;background:rgba(4,7,14,.74);backdrop-filter:blur(8px)}
.wf-card{text-align:center;position:relative;padding:50px 74px;border-radius:16px;background:linear-gradient(160deg,rgba(13,20,38,.97),rgba(7,11,22,.97));border:1px solid rgba(233,196,106,.5);animation:cardIn .5s ease both}
@keyframes cardIn{from{opacity:0;transform:scale(.9)}to{opacity:1;transform:none}}
.wf-glow{position:absolute;inset:-30px;border-radius:50%;pointer-events:none;background:radial-gradient(circle,rgba(233,196,106,.3),transparent 65%);animation:glowPulse 1.2s ease-in-out infinite}
@keyframes glowPulse{50%{transform:scale(1.12);opacity:.7}}
.wf-crown{font-size:44px;color:#e9c46a;text-shadow:0 0 22px rgba(233,196,106,.6);margin:0}
.wf-title{font-family:"ZCOOL XiaoWei","Noto Serif SC",serif;font-size:38px;font-weight:900;letter-spacing:10px;background:linear-gradient(90deg,#e9c46a,#ffffff,#e9c46a);-webkit-background-clip:text;background-clip:text;color:transparent;margin:8px 0}
.wf-sub{font-size:12px;color:#9fb0c8;letter-spacing:3px}.wf-gain{font-size:16px;color:#e9c46a;letter-spacing:2px;margin-top:6px}

.toast{position:fixed;left:50%;top:84px;transform:translateX(-50%) translateY(-20px);background:linear-gradient(135deg,#0f766e,#2dd4bf);color:#e8eef7;padding:11px 22px;border-radius:10px;
  font-weight:700;letter-spacing:1px;opacity:0;pointer-events:none;transition:.3s;z-index:95;box-shadow:0 14px 40px rgba(45,212,191,.35);max-width:80vw}
.toast.show{opacity:1;transform:translateX(-50%) translateY(0)}
.pop-enter-active,.pop-leave-active{transition:.3s}.pop-enter-from,.pop-leave-to{opacity:0;transform:translateY(-8px)}
.fade-enter-active,.fade-leave-active{transition:opacity .35s}.fade-enter-from,.fade-leave-to{opacity:0}

@media(max-width:1040px){.arena{grid-template-columns:1fr}.statusbar{grid-template-columns:repeat(2,1fr)}.extras{grid-template-columns:1fr}}
@media(max-width:680px){.board{grid-template-columns:repeat(4,1fr)}.crumbs{display:none}.nav-right .pill{display:none}}
</style>
