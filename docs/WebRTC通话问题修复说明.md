# WebRTC 通话问题修复说明

## 修复的问题

### 1. 对方没有收到呼叫
**问题原因：**
- 信令处理逻辑有缺陷，只处理当前聊天用户的信令
- 来电时没有正确切换到发起通话的用户

**修复方案：**
```javascript
// 修复前：只处理当前聊天用户的信令
if (fromUserId !== chatUser.value.id) return

// 修复后：来电邀请可以来自任何用户
if (fromUserId !== chatUser.value.id && data.type !== 'call-offer') {
  return
}

// 收到来电时自动切换到发起通话的用户
case 'call-offer':
  const callerUser = users.value.find(user => user.id === fromUserId)
  if (callerUser) {
    changeUser(callerUser) // 切换到发起通话的用户
    incomingCall.value = data
    showIncomingCallDialog.value = true
  }
```

### 2. 时间计算有误
**问题原因：**
- 发起通话时就开始计时，而不是等对方接听后
- 应该在双方建立连接后才开始计时

**修复方案：**
```javascript
// 发起通话时不开始计时
const startCall = async () => {
  // ... 创建连接逻辑
  isInCall.value = true
  isCallInitiator.value = true
  // 移除：startCallTimer() - 不在这里开始计时
  ElMessage.success('正在呼叫对方...')
}

// 发起方收到应答后开始计时
case 'call-answer':
  if (peerConnection && isCallInitiator.value) {
    await peerConnection.setRemoteDescription(data.answer)
    startCallTimer() // 在这里开始计时
    ElMessage.success('通话已接通')
  }

// 接听方接听后开始计时
const answerCall = async () => {
  // ... 接听逻辑
  startCallTimer() // 接听后立即开始计时
}
```

## 新增功能

### 1. 通话状态管理
添加了详细的通话状态跟踪：
```javascript
const callStatus = ref('') // 'calling', 'ringing', 'connected', 'ended'
```

**状态说明：**
- `calling`: 发起通话，等待对方响应
- `ringing`: 收到来电，正在响铃
- `connected`: 通话已接通
- `ended`: 通话结束

### 2. 改进的UI显示
根据通话状态显示不同信息：
```javascript
// 动态显示通话状态
{{ callStatus === 'connected' ? formatCallDuration(callDuration) : 
   callStatus === 'calling' ? '呼叫中...' : 
   callStatus === 'ringing' ? '响铃中...' : '00:00' }}

// 只在通话接通后显示静音按钮
<el-button v-if="callStatus === 'connected'">
```

### 3. 增强的调试日志
添加了详细的调试信息：
```javascript
console.log('发送WebRTC信令:', data.type, 'to user:', chatUser.value.id)
console.log('收到WebRTC信令:', data.type, 'from user:', fromUserId)
```

## 修复后的通话流程

### 发起通话流程
1. 用户A点击"语音通话"按钮
2. 状态变为 `calling`，显示"呼叫中..."
3. 发送 `call-offer` 信令给用户B
4. 等待用户B的响应

### 接听通话流程
1. 用户B收到 `call-offer` 信令
2. 自动切换到用户A的聊天窗口
3. 状态变为 `ringing`，显示来电弹窗
4. 用户B点击接听，发送 `call-answer` 信令
5. 状态变为 `connected`，开始计时

### 通话建立流程
1. 用户A收到 `call-answer` 信令
2. 状态变为 `connected`，开始计时
3. 双方可以进行语音通话
4. 显示通话时长和控制按钮

## 测试验证

### 测试场景
1. **跨用户来电测试**
   - 用户A给用户B发起通话
   - 验证用户B能收到来电弹窗
   - 验证自动切换到用户A的聊天窗口

2. **计时准确性测试**
   - 发起通话时不应开始计时
   - 对方接听后才开始计时
   - 验证双方显示的时长一致

3. **状态显示测试**
   - 发起方显示"呼叫中..."
   - 接听方显示"响铃中..."
   - 接通后显示实际通话时长

### 调试方法
1. 打开浏览器开发者工具
2. 查看控制台日志，确认信令发送和接收