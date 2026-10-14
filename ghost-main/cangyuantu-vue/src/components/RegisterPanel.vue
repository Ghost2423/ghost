<template>
  <section class="panel">
    <form class="card" id="regForm" autocomplete="off" @submit.prevent="onSubmit">
      <div class="ttl">神尊殿</div>
      <div class="sub">凝元塑身，铸就不灭神格</div>
      <div class="forge">锻造神尊根基</div>
      <div class="divider"></div>

      <!-- 注册表单主体 -->
      <div id="formBody" v-show="!forged">
        <div class="field">
          <label for="account">账号</label>
          <div class="control">
            <input id="account" type="text" maxlength="20" v-model="account" placeholder="数字 / 字母 / 汉字 / 英文 均可" />
          </div>
          <div class="hint">账号可自由设置，支持数字、字母、汉字或英文任意组合。</div>
        </div>

        <div class="field">
          <label for="password">密码</label>
          <div class="control">
            <input id="password" :type="showPwd ? 'text' : 'password'" maxlength="32"
                   v-model="password" placeholder="设置你的密码（至少 6 位）" />
            <span class="ico" title="显示/隐藏" @click="showPwd = !showPwd">👁</span>
          </div>
          <div class="hint" :class="{ err: pwdErr }">{{ pwdHint }}</div>
        </div>

        <div class="row">
          <label><input type="checkbox" id="agree" v-model="agree" /> 立下心魔誓约（用户协议）</label>
        </div>

        <div class="captcha">
          <label class="cap-label">神纹验真（验证码自动刷新）</label>
          <div class="cap-row">
            <span class="code" title="点击刷新神纹" @click="genCaptcha">{{ capCode }}</span>
            <button class="refresh" type="button" title="刷新验证码" @click="genCaptcha">↻ 换纹</button>
            <input class="cap-input" :class="{ err: capErr }" type="text" maxlength="4"
                   placeholder="输入神纹" autocomplete="off" v-model="capInput" />
          </div>
        </div>

        <button class="btn" type="submit" :disabled="loading">{{ loading ? '锻 造 中…' : '锻造神尊根基' }}</button>

        <div class="switch">已有根基？<a href="#" @click.prevent="$emit('go-login')">返回觉醒 ›</a></div>
      </div>

      <!-- 根基已成态（数据来自后端入库结果） -->
      <div class="awaken" :class="{ show: forged }">
        <div class="halo"><div class="portrait"><img :src="tianzunImg" alt="神尊" /></div><span class="flash"></span></div>
        <div class="name">神尊根基已成</div>
        <span class="ink-stroke"></span>
        <div class="desc">觉醒神尊 · {{ forgedName }}</div>
        <div class="desc">你的账号 · {{ forgedAccount }}</div>
        <div class="myth">{{ forgedMyth }}</div>
        <button class="reset" type="button" @click="$emit('go-login')">前往觉醒</button>
      </div>
    </form>

    <!-- 提示 toast -->
    <div class="toast" :class="{ show: toastShow }">{{ toastMsg }}</div>
  </section>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { forgeAvatar } from '../api'
import tianzunImg from '../assets/tianzun.jpg'

defineEmits(['go-login'])

// ===== 上古神话神尊原型库（依据账号关键词觉醒，仅作展示封号用）=====
const MYTHS = [
  { kw:["盘","混沌","原","元","洪"], name:"盘古",     story:"开天辟祖，清浊自分，撑天地于洪荒。", spell:"盘古开天，清浊自分" },
  { kw:["女","土","造","冀","人"],   name:"女娲",     story:"抟土造人，炼石补天，庇佑苍生。",     spell:"女娲炼石，以补苍天" },
  { kw:["伏","卦","河","洛","陇"],   name:"伏羲",     story:"演八卦而定四时，启文明之始。",       spell:"伏羲演卦，统御八荒" },
  { kw:["神","草","谷","楚","农"],   name:"神农",     story:"尝百草、播五谷，惠泽万民。",         spell:"神农尝草，惠泽苍生" },
  { kw:["黄","轩","冀","涿","中"],   name:"黄帝",     story:"中央天帝，战蚩尤而一统寰宇。",       spell:"黄帝临世，万灵归心" },
  { kw:["炎","火","姜","南"],       name:"炎帝",     story:"火德之帝，教民耕织。",             spell:"炎帝司火，燎原照世" },
  { kw:["祝","融","火","南","楚"],   name:"祝融",     story:"南方火神，秉烈焰焚尽邪祟。",         spell:"祝融秉火，焚尽邪祟" },
  { kw:["共","工","水","北","洪"],   name:"共工",     story:"怒触不周，天倾西北。",             spell:"共工怒涛，倾覆不周" },
  { kw:["后","土","地","坤","厚"],   name:"后土",     story:"大地之母，厚载万物。",             spell:"后土承地，厚载万物" },
  { kw:["东","海","太","一","皇"],   name:"东皇太一", story:"九歌所载上古天帝，统御东方。",       spell:"东皇太一，统御八荒" },
  { kw:["玄","战","兵","女"],       name:"九天玄女", story:"授兵符战法，破阵千里。",           spell:"九天玄女临，授符破阵" },
  { kw:["烛","钟","极","北","龙"],   name:"烛龙",     story:"衔烛而照幽都，睁眼为昼。",         spell:"烛龙衔烛，昼夜由心" },
  { kw:["木","春","句","芒","东"],   name:"句芒",     story:"东方木神，司春而草木同苏。",         spell:"句芒司春，草木同苏" },
  { kw:["日","弓","羿","夏","后"],   name:"后羿",     story:"弯弓射日，拯民于酷旱。",           spell:"后羿弯弓，九日俱沉" },
  { kw:["刑","战","勇","戚","常"],   name:"刑天",     story:"断首犹舞干戚，虽殁犹战。",         spell:"刑天舞戚，虽殁犹战" },
  { kw:["西","昆","瑶","母"],       name:"西王母",   story:"昆仑之主，瑶池生辉。",             spell:"西王母临，瑶池生辉" }
]

const TITLES = ["真君","天君","道尊","元君","星君","上仙","灵尊","神君"]
const rnd = arr => arr[Math.floor(Math.random() * arr.length)]

function pickMyth(seed) {
  for (const m of MYTHS) { if (m.kw.some(k => seed.includes(k))) return m }
  let h = 0
  for (const c of seed) h = (h * 31 + c.charCodeAt(0)) >>> 0
  return MYTHS[h % MYTHS.length]
}

// 锻造神尊封号（仅展示用，依据账号派生上古神尊之名）
function forgeSeal(seed) {
  const m = pickMyth(seed)
  return (m.name + rnd(TITLES)).slice(0, 9)
}

// ===== 表单状态 =====
const account = ref('')
const password = ref('')
const showPwd = ref(false)
const agree = ref(false)
const loading = ref(false)
const forged = ref(false)
const forgedName = ref('')
const forgedAccount = ref('')
const forgedMyth = ref('')

const pwdErr = ref(false)
const pwdHint = ref('密码至少 6 位，支持数字、字母、汉字或英文任意组合。')

// ===== 验证码（神纹）=====
const CAP_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
const capCode = ref('')
const capInput = ref('')
const capErr = ref(false)
let capTimer = null

function genCaptcha() {
  let s = ""
  for (let i = 0; i < 4; i++) s += CAP_CHARS[Math.floor(Math.random() * CAP_CHARS.length)]
  capCode.value = s
}
onMounted(() => {
  genCaptcha()
  capTimer = setInterval(genCaptcha, 30000) // 每 30 秒自行更迭
})
onBeforeUnmount(() => { if (capTimer) clearInterval(capTimer) })

// ===== toast =====
const toastMsg = ref('')
const toastShow = ref(false)
let toastTimer = null
function toast(msg) {
  toastMsg.value = msg
  toastShow.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toastShow.value = false }, 2200)
}
onBeforeUnmount(() => { if (toastTimer) clearTimeout(toastTimer) })

// 锻造神尊根基（注册）—— 走后端 /api/forge，写入 MySQL
// 账号即为登录名（deity_name），密码即为召唤语（spell）
async function onSubmit() {
  const a = account.value.trim()
  const p = password.value
  const cap = capInput.value.trim().toUpperCase()

  if (!a) return toast('请填写账号')
  if (!agree.value) return toast('请先立下心魔誓约')

  const len = [...p].length
  if (!len) return toast('请设置密码')
  if (len < 6) {
    pwdErr.value = true
    pwdHint.value = '密码至少需 6 位，当前 ' + len + ' 位。'
    return toast('密码至少 6 位')
  }
  pwdErr.value = false

  if (cap !== capCode.value) {
    capErr.value = true
    toast('神纹验真未过，请重新输入')
    genCaptcha()
    capInput.value = ''
    return
  }
  capErr.value = false

  // 依据账号派生一个上古神尊封号（仅展示，不影响登录身份）
  const seal = forgeSeal(a)
  const myth = pickMyth(a)

  const payload = {
    region: a,
    deity_name: a,        // 账号即登录名
    myth_name: seal,      // 神尊封号（展示用）
    spell: p,             // 密码即召唤语
    myth_story: myth.name + '：' + myth.story,
    agreed: agree.value ? 1 : 0
  }

  loading.value = true
  const d = await forgeAvatar(payload)
  loading.value = false

  // 显现锻造结果
  forgedName.value = seal
  forgedAccount.value = a
  forgedMyth.value = myth.name + '：' + myth.story
  forged.value = true

  if (d.ok) {
    toast('神尊根基已锻造 · 已录入元界数据库')
  } else {
    toast('根基已成，数据库：' + (d.msg || '写入失败'))
  }
}
</script>

<style scoped>
/* 神尊殿标题 */
.forge { text-align: center; margin-top: 14px; font-size: 14px; color: var(--gold);
  letter-spacing: 6px; text-shadow: 0 0 12px rgba(233, 196, 106, .45); }

/* 提示 */
.hint { font-size: 12px; color: var(--muted); margin-top: 6px; letter-spacing: .5px; line-height: 1.7; }
.hint.err { color: var(--crimson); }

/* 密码显隐 */
.ico { position: absolute; right: 13px; top: 50%; transform: translateY(-50%);
  color: var(--muted); cursor: pointer; user-select: none; font-size: 14px; }
.ico:hover { color: var(--jade); }

/* 验证码（神纹） */
.captcha { margin: 4px 0 20px; }
.captcha .cap-label { display: block; font-size: 13px; color: var(--muted); margin-bottom: 8px; letter-spacing: 1px; }
.captcha .cap-row { display: flex; align-items: center; gap: 10px; }
.captcha .code { font-family: "ZCOOL XiaoWei", monospace; font-size: 23px; letter-spacing: 7px;
  padding: 10px 16px; border-radius: 11px; color: #04130f; font-weight: 900; min-width: 118px; text-align: center;
  background: linear-gradient(135deg, var(--jade), #5eead4);
  box-shadow: 0 8px 22px rgba(45, 212, 191, .35); user-select: none; cursor: pointer;
  position: relative; overflow: hidden; transition: .18s; }
.captcha .code:hover { filter: brightness(1.06); }
.captcha .code::after { content: ""; position: absolute; inset: 0;
  background: repeating-linear-gradient(0deg, rgba(0, 0, 0, .07) 0 2px, transparent 2px 5px); }
.captcha .refresh { background: none; border: 1px solid var(--line); color: var(--jade);
  border-radius: 9px; padding: 9px 12px; cursor: pointer; font-size: 13px; transition: .15s; white-space: nowrap;
  font-family: inherit; }
.captcha .refresh:hover { background: rgba(45, 212, 191, .12); }
.captcha .cap-input { flex: 1; min-width: 0; padding: 13px 14px; font-size: 15px; letter-spacing: 5px;
  background: rgba(7, 11, 22, .6); border: 1px solid rgba(255, 255, 255, .12); border-radius: 11px;
  color: var(--text); outline: none; font-family: inherit; text-transform: uppercase; }
.captcha .cap-input:focus { border-color: var(--jade); box-shadow: 0 0 0 3px rgba(45, 212, 191, .18); }
.captcha .cap-input.err { border-color: var(--crimson); box-shadow: 0 0 0 3px rgba(239, 68, 84, .18); }

/* 切换链接 */
.switch { text-align: center; margin-top: 18px; font-size: 13px; color: var(--muted); letter-spacing: 1px; }
.switch a { color: var(--jade); text-decoration: none; margin-left: 4px; }
.switch a:hover { text-decoration: underline; }

/* 觉醒态神话简介 */
.awaken .myth { color: var(--muted); font-size: 12.5px; margin: 16px auto 0; max-width: 312px;
  line-height: 1.95; letter-spacing: .5px; border-top: 1px solid var(--line); padding-top: 14px; }
.btn:disabled { opacity: .6; cursor: wait; }
</style>
