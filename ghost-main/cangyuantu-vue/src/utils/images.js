/* =============================================================
 * 全站图片资源中心
 * -------------------------------------------------------------
 * 所有图片必须通过这里取用：Vite 会把 assets 下的文件编译成带哈希的正式 URL，
 * 开发 / 打包 / 部署后都能显示。禁止在页面里写死 /src/assets/... 或
 * /generated-images/... 这类本地磁盘路径（打包后必然 404）。
 * ============================================================= */

const modules = {
  ...import.meta.glob('../assets/images/*.webp', { eager: true, import: 'default' }),
  ...import.meta.glob('../assets/*.jpg', { eager: true, import: 'default' })
}

/** 文件名（不含扩展名）→ 可访问的图片 URL */
export const imageMap = {}
for (const [path, url] of Object.entries(modules)) {
  const name = path.split('/').pop().replace(/\.(webp|jpe?g|png)$/i, '')
  imageMap[name] = url
}

/** 首页英雄区 / 四大界域展示图 */
export const heroImage = imageMap.hero || ''
export const sectionImage = {
  '神尊': imageMap['sec-deity'] || '',
  '功法': imageMap['sec-gongfa'] || '',
  '妖魔': imageMap['sec-demon'] || '',
  '秘境': imageMap['sec-realm'] || ''
}

/** 各界面统一背景：登录 / 修炼 / 弈局 / 论坛 / 后台 */
export const pageImages = {
  auth: imageMap.hero || imageMap['sec-deity'] || '',
  home: heroImage,
  cultivation: imageMap['sec-gongfa'] || heroImage,
  boardgame: imageMap['sec-realm'] || heroImage,
  forum: imageMap['sec-deity'] || heroImage,
  admin: imageMap.tianzun || imageMap.hero || ''
}

export function getImage(name) {
  return imageMap[name] || ''
}

/**
 * 后端条目的 image 字段兼容处理：
 * 后台若填了 /src/assets/images/xxx.webp，打包后会失效，这里自动换成正式 URL。
 */
function normalizeImage(path) {
  if (!path) return ''
  const str = String(path)
  const match = str.match(/(?:^|\/)(?:src\/)?assets\/images\/([^/]+?)\.(webp|jpe?g|png)(?:\?.*)?$/i)
  if (match && imageMap[match[1]]) return imageMap[match[1]]
  const plain = str.match(/^([^/?#]+)\.(webp|jpe?g|png)$/i)
  if (plain && imageMap[plain[1]]) return imageMap[plain[1]]
  return str
}

/** 首页卡片配图：后台自定义 > codex_{id} > 分类图 */
export function itemImage(it) {
  if (!it) return ''
  if (it.image) return normalizeImage(it.image)
  if (it.id != null && imageMap['codex_' + it.id]) return imageMap['codex_' + it.id]
  return sectionImage[it.type] || ''
}
