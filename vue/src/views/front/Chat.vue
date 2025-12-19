<script setup>
import {ref, onBeforeUnmount, nextTick} from 'vue'
import {useRouter, useRoute} from 'vue-router'
import request from '../../utils/request'
import {ip, serverHost} from '../../../config/config.default'
import {ElMessage, ElMessageBox} from 'element-plus'
import {ChatRound, UploadFilled, Picture, Position, Microphone, MuteNotification, Phone, Close, VideoCamera, VideoCameraFilled, Sunny} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const account = ref(localStorage.getItem('account') ? JSON.parse(localStorage.getItem('account')) : {})
const chatUser = ref({})
const users = ref([])
const userIds = ref([])
const messages = ref([])
const text = ref('')
const userId = ref(route.query.userId)
const messagesContainer = ref(null)
const localVideoRef = ref(null)
const remoteVideoRef = ref(null)

// WebRTC 通话相关状态
const isInCall = ref(false)
const isCallInitiator = ref(false)
const isMuted = ref(false)
const isVideoOff = ref(false)
const callDuration = ref(0)
const callTimer = ref(null)
const incomingCall = ref(null)
const showIncomingCallDialog = ref(false)
const callStatus = ref('') // 'calling', 'ringing', 'connected', 'ended'
const callStartTime = ref(null) // 通话开始时间
const callType = ref('audio') // 'audio' 或 'video'
const showVideoCall = ref(false) // 是否显示视频通话界面

// 表情功能相关状态
const showEmojiPanel = ref(false)
const emojiCategories = ref([
  {
    name: '笑脸',
    key: 'smileys',
    emojis: ['😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂', '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩', '😘', '😗', '😚', '😙', '😋', '😛', '😜', '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐', '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬', '🤥']
  },
  {
    name: '手势',
    key: 'gestures',
    emojis: ['👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙', '👈', '👉', '👆', '🖕', '👇', '☝️', '👋', '🤚', '🖐️', '✋', '🖖', '👏', '🙌', '🤲', '🤝', '🙏', '✍️', '💅', '🤳', '💪', '🦾', '🦿', '🦵', '🦶', '👂', '🦻', '👃', '🧠', '🦷', '🦴', '👀', '👁️', '👅', '👄']
  },
  {
    name: '心情',
    key: 'emotions',
    emojis: ['❤️', '🧡', '💛', '💚', '💙', '💜', '🖤', '🤍', '🤎', '💔', '❣️', '💕', '💞', '💓', '💗', '💖', '💘', '💝', '💟', '☮️', '✝️', '☪️', '🕉️', '☸️', '✡️', '🔯', '🕎', '☯️', '☦️', '🛐', '⛎', '♈', '♉', '♊', '♋', '♌', '♍', '♎', '♏', '♐']
  },
  {
    name: '动物',
    key: 'animals',
    emojis: ['🐶', '🐱', '🐭', '🐹', '🐰', '🦊', '🐻', '🐼', '🐨', '🐯', '🦁', '🐮', '🐷', '🐽', '🐸', '🐵', '🙈', '🙉', '🙊', '🐒', '🐔', '🐧', '🐦', '🐤', '🐣', '🐥', '🦆', '🦅', '🦉', '🦇', '🐺', '🐗', '🐴', '🦄', '🐝', '🐛', '🦋', '🐌', '🐞', '🐜', '🦟', '🦗', '🕷️', '🦂', '🐢', '🐍', '🦎', '🦖', '🦕', '🐙', '🦑', '🦐', '🦞', '🦀', '🐡', '🐠', '🐟', '🐬', '🐳', '🐋', '🦈', '🐊', '🐅', '🐆', '🦓', '🦍', '🦧', '🐘', '🦛', '🦏', '🐪', '🐫', '🦒', '🦘', '🐃', '🐂', '🐄', '🐎', '🐖', '🐏', '🐑', '🦙', '🐐', '🦌', '🐕', '🐩', '🦮', '🐕‍🦺', '🐈', '🐓', '🦃', '🦚', '🦜', '🦢', '🦩', '🕊️', '🐇', '🦝', '🦨', '🦡', '🦦', '🦥', '🐁', '🐀', '🐿️']
  },
  {
    name: '食物',
    key: 'food',
    emojis: ['🍎', '🍐', '🍊', '🍋', '🍌', '🍉', '🍇', '🍓', '🫐', '🍈', '🍒', '🍑', '🥭', '🍍', '🥥', '🥝', '🍅', '🍆', '🥑', '🥦', '🥬', '🥒', '🌶️', '🫑', '🌽', '🥕', '🫒', '🧄', '🧅', '🥔', '🍠', '🥐', '🥯', '🍞', '🥖', '🥨', '🧀', '🥚', '🍳', '🧈', '🥞', '🧇', '🥓', '🥩', '🍗', '🍖', '🦴', '🌭', '🍔', '🍟', '🍕']
  },
  {
    name: '活动',
    key: 'activities',
    emojis: ['⚽', '🏀', '🏈', '⚾', '🥎', '🎾', '🏐', '🏉', '🥏', '🎱', '🪀', '🏓', '🏸', '🏒', '🏑', '🥍', '🏏', '🪃', '🥅', '⛳', '🪁', '🏹', '🎣', '🤿', '🥊', '🥋', '🎽', '🛹', '🛷', '⛸️', '🥌', '🎿', '⛷️', '🏂', '🪂', '🏋️‍♀️', '🏋️', '🏋️‍♂️', '🤼‍♀️', '🤼', '🤼‍♂️', '🤸‍♀️', '🤸', '🤸‍♂️', '⛹️‍♀️', '⛹️', '⛹️‍♂️', '🤺', '🤾‍♀️', '🤾', '🤾‍♂️', '🏌️‍♀️', '🏌️', '🏌️‍♂️', '🏇', '🧘‍♀️', '🧘', '🧘‍♂️', '🏄‍♀️', '🏄', '🏄‍♂️', '🏊‍♀️', '🏊', '🏊‍♂️', '🤽‍♀️', '🤽', '🤽‍♂️', '🚣‍♀️', '🚣', '🚣‍♂️', '🧗‍♀️', '🧗', '🧗‍♂️', '🚵‍♀️', '🚵', '🚵‍♂️', '🚴‍♀️', '🚴', '🚴‍♂️']
  }
])
const activeEmojiCategory = ref('smileys')

// WebRTC 相关对象
let localStream = null
let remoteStream = null
let peerConnection = null
let localAudio = null
let remoteAudio = null
let localVideo = null
let remoteVideo = null

// WebRTC 配置
const rtcConfiguration = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' }
  ]
}

const loadUser = () => {
  request.get('/chat/user').then(res => {
    users.value = res.data
    init()
    //如果是聊一聊进来的，那么判断一下是否和当前用户沟通过
    if (userId.value) {
      //如果一个也没沟通过，直接创建一个新的对话
      if (users.value.length === 0) {
        request.get('/chat/user/' + userId.value).then(res => {
          const user = res.data
          users.value.unshift(res.data)
          changeUser(user)
          init()
        })
      } else {
        //如果以前和人沟通过，判断以前是否是存在对话记录
        let user = users.value.find(item => item.id === Number(userId.value))
        //没有对话记录，创建一个新的对话
        if (!user) {
          request.get('/chat/user/' + userId.value).then(res => {
            user = res.data
            users.value.unshift(res.data)
            changeUser(user)
            init()
          })
        } else {
          //如果沟通过，直接跳转
          changeUser(user)
          init()
        }
      }
    }
  })
}
loadUser()

let socket = null

const changeUser = (user) => {

  if (chatUser.value.id !== user.id) {
    chatUser.value = user
    users.value.find(item => item.id === user.id).count = 0
    loadMessage(account.value.id, chatUser.value.id)
  }

}

const loadMessage = (fromUserId, toUserId) => {
  request.get('/chat/message', {
    params: {
      fromUserId: fromUserId,
      toUserId: toUserId
    }
  }).then(res => {
    messages.value = res.data
    scrollToBottom()
    clear()
  })
}

const init = () => {
  const userId = account.value.id
  const socketUrl = "ws://"+ip+":9090/chatServer/" + userId;

  // 开启一个websocket服务
  socket = new WebSocket(socketUrl)

  // 打开事件
  socket.onopen = () => {
    console.log("websocket已打开")
  }

  // 接受消息事件
  socket.onmessage = (msg) => {
    const data = JSON.parse(msg.data)
    //如果发来的数据类型是广播，广播给所有在线连接:当前所有在线用户id
    if (data.messageType === 'broadcast') {
      userIds.value = data.userIds
      users.value.forEach(item => {
        // 如果用户id在userIds数组中，设置online为true，否则为false
        item.online = userIds.value.includes(item.id)
      })
    }
    //如果发来的数据类型是聊天，正常执行聊天渲染逻辑
    else if (data.messageType === 'chat') {
      // 只有发来数据用户是和当前正在聊天用户匹配时候，再去给页面上加数据
      if (data.fromUserId === chatUser.value.id) {
        const message = {
          text: data.text,
          type: data.type,
          time: data.time,
          fromUserId: data.fromUserId,
          toUserId: data.toUserId,
          isRead: false,
        }
        createMessage(message)
      } else {
        //否则需要先判断聊天列表里是否有历史记录
        let user = users.value.find(item => item.id === Number(data.fromUserId))
        //如果有，则直接设置未读+1
        if (user) {
          users.value.find(item => item.id === data.fromUserId).count++
        } else {
          //如果没有，就开一个新会话,并且判断一下，这个用户是否在线，设置在线情况，同时未读设置为1
          request.get('/chat/user/' + data.fromUserId).then(res => {
            user = res.data
            user.count = 1
            user.online = userIds.value.includes(data.fromUserId)
            users.value.unshift(user)
          })
        }
      }
    }
    // WebRTC 信令处理
    else if (data.messageType === 'webrtc') {
      handleWebRTCSignaling(data)
    }
  }

  // 错误事件
  socket.onerror = (error) => {
    console.error("WebSocket错误:", error)
  }
}

const sendMessage = () => {
  if (!chatUser.value.id) {
    ElMessage({type: 'warning', message: "请选择聊天对象"})
    return
  }
  if (!text.value) {
    ElMessage({type: 'warning', message: "请输入内容"})
    return
  }

  const message = {
    text: text.value,
    type: '文字',
    time: new Date().toLocaleString('zh-cn'),
    fromUserId: account.value.id,
    toUserId: chatUser.value.id,
    isRead: false
  }

  socket.send(JSON.stringify(message))
  createMessage(message)
  saveMessage(message)
  text.value = ''
  showEmojiPanel.value = false // 发送消息后关闭表情面板
}

const sendImgMessage = (res) => {
  // 从上传返回的结果中获取URL
  const imgUrl = typeof res === 'string' ? res : (res.data || res)
  const message = {
    text: imgUrl,
    type: '图片',
    time: new Date().toLocaleString('zh-cn'),
    fromUserId: account.value.id,
    toUserId: chatUser.value.id,
    isRead: false
  }
  socket.send(JSON.stringify(message))
  createMessage(message)
  saveMessage(message)
}

const sendFileMessage = (res) => {
  // 从上传返回的结果中获取URL
  const fileUrl = typeof res === 'string' ? res : (res.data || res)
  const message = {
    text: fileUrl,
    type: '文件',
    time: new Date().toLocaleString('zh-cn'),
    fromUserId: account.value.id,
    toUserId: chatUser.value.id,
    isRead: false
  }
  socket.send(JSON.stringify(message))
  createMessage(message)
  saveMessage(message)
}

const saveMessage = (message) => {
  request.post('/chat', message).then(res => {
    clear()
  })
}

const getFileNameFromUrl = (url) => {
  // 从URL中提取文件名
  if (!url) return '未知文件'

  try {
    // 如果是OSS URL，提取最后一个斜杠后的部分
    const fileName = url.split('/').pop()

    // 检查是否是编码格式：UUID_编码后的原始文件名
    if (fileName.includes('_')) {
      const parts = fileName.split('_')
      if (parts.length >= 2) {
        // 第一部分是UUID，从第二部分开始是编码后的原始文件名
        const encodedOriginalName = parts.slice(1).join('_')
        try {
          // 尝试URL解码，获取原始中文文件名
          return decodeURIComponent(encodedOriginalName)
        } catch (e) {
          // 解码失败，返回编码后的文件名
          return encodedOriginalName
        }
      }
    }

    // 如果不是编码格式，尝试直接解码（兼容旧格式）
    if (fileName.includes('.')) {
      try {
        return decodeURIComponent(fileName)
      } catch (e) {
        return fileName
      }
    }

    return fileName || '未知文件'
  } catch (e) {
    return '未知文件'
  }
}

const downloadFile = (fileUrl) => {
  if (!fileUrl) return
  // 直接打开链接，让浏览器处理下载
  window.open(fileUrl, '_blank')
}

const createMessage = (message) => {
  messages.value.push(message)
  scrollToBottom()
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const clear = () => {
  request.get('/chat/clear', {
    params: {
      fromUserId: account.value.id,
      toUserId: chatUser.value.id
    }
  })
}

onBeforeUnmount(() => {
  if (socket) {
    socket.onclose = () => {
      console.log("websocket已关闭")
    }
    socket.close()
  }

  // 清理表情面板事件监听器
  document.removeEventListener('click', handleClickOutside)
})

const handleKeydown = (e) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

// 表情功能
const toggleEmojiPanel = () => {
  showEmojiPanel.value = !showEmojiPanel.value
}

const selectEmoji = (emoji) => {
  text.value += emoji
  // 可以选择是否在选择表情后关闭面板
  // showEmojiPanel.value = false
}

const switchEmojiCategory = (categoryKey) => {
  activeEmojiCategory.value = categoryKey
}

// 点击外部关闭表情面板
const handleClickOutside = (event) => {
  const emojiPanel = document.querySelector('.emoji-panel')
  const emojiButton = document.querySelector('.emoji-button')

  if (emojiPanel && emojiButton &&
      !emojiPanel.contains(event.target) &&
      !emojiButton.contains(event.target)) {
    showEmojiPanel.value = false
  }
}

// 监听点击事件
document.addEventListener('click', handleClickOutside)

// WebRTC 通话功能
const initializeMediaElements = (isVideo = false) => {
  if (!localAudio) {
    localAudio = document.createElement('audio')
    localAudio.muted = true // 本地音频静音避免回音
  }
  if (!remoteAudio) {
    remoteAudio = document.createElement('audio')
    remoteAudio.autoplay = true
    document.body.appendChild(remoteAudio)
  }

  if (isVideo) {
    if (!localVideo) {
      localVideo = document.createElement('video')
      localVideo.muted = true
      localVideo.autoplay = true
      localVideo.playsInline = true
    }
    if (!remoteVideo) {
      remoteVideo = document.createElement('video')
      remoteVideo.autoplay = true
      remoteVideo.playsInline = true
    }
  }
}

const startCall = async (isVideo = false) => {
  if (!chatUser.value.id) {
    ElMessage.warning('请选择通话对象')
    return
  }

  if (!chatUser.value.online) {
    ElMessage.warning('对方不在线，无法发起通话')
    return
  }

  try {
    callType.value = isVideo ? 'video' : 'audio'
    initializeMediaElements(isVideo)

    // 获取用户媒体流
    const constraints = isVideo
        ? { audio: true, video: true }
        : { audio: true }

    localStream = await navigator.mediaDevices.getUserMedia(constraints)

    if (isVideo) {
      showVideoCall.value = true
      // 等待DOM更新后绑定视频流
      nextTick(() => {
        if (localVideoRef.value) {
          localVideoRef.value.srcObject = localStream
        }
      })
    } else {
      localAudio.srcObject = localStream
    }

    // 创建 RTCPeerConnection
    peerConnection = new RTCPeerConnection(rtcConfiguration)

    // 添加本地流到连接
    localStream.getTracks().forEach(track => {
      peerConnection.addTrack(track, localStream)
    })

    // 处理远程流
    peerConnection.ontrack = (event) => {
      remoteStream = event.streams[0]
      if (isVideo) {
        nextTick(() => {
          if (remoteVideoRef.value) {
            remoteVideoRef.value.srcObject = remoteStream
          }
        })
      } else {
        remoteAudio.srcObject = remoteStream
      }
    }

    // 处理 ICE 候选
    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        sendWebRTCMessage({
          type: 'ice-candidate',
          candidate: event.candidate
        })
      }
    }

    // 创建 offer
    const offer = await peerConnection.createOffer()
    await peerConnection.setLocalDescription(offer)

    // 发送通话邀请
    sendWebRTCMessage({
      type: 'call-offer',
      offer: offer,
      callType: callType.value
    })

    isInCall.value = true
    isCallInitiator.value = true
    callStatus.value = 'calling'
    // 不在这里开始计时，等对方接听后再开始

    ElMessage.success(`正在发起${isVideo ? '视频' : '语音'}通话...`)
  } catch (error) {
    console.error('发起通话失败:', error)
    ElMessage.error(`发起通话失败，请检查${isVideo ? '摄像头和麦克风' : '麦克风'}权限`)
  }
}

const answerCall = async () => {
  try {
    const isVideo = callType.value === 'video'
    initializeMediaElements(isVideo)

    // 获取用户媒体流
    const constraints = isVideo
        ? { audio: true, video: true }
        : { audio: true }

    localStream = await navigator.mediaDevices.getUserMedia(constraints)

    if (isVideo) {
      showVideoCall.value = true
      // 等待DOM更新后绑定视频流
      nextTick(() => {
        if (localVideoRef.value) {
          localVideoRef.value.srcObject = localStream
        }
      })
    } else {
      localAudio.srcObject = localStream
    }

    // 创建 RTCPeerConnection
    peerConnection = new RTCPeerConnection(rtcConfiguration)

    // 添加本地流到连接
    localStream.getTracks().forEach(track => {
      peerConnection.addTrack(track, localStream)
    })

    // 处理远程流
    peerConnection.ontrack = (event) => {
      remoteStream = event.streams[0]
      if (isVideo) {
        remoteVideo.srcObject = remoteStream
      } else {
        remoteAudio.srcObject = remoteStream
      }
    }

    // 处理 ICE 候选
    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        sendWebRTCMessage({
          type: 'ice-candidate',
          candidate: event.candidate
        })
      }
    }

    // 处理远程流
    peerConnection.ontrack = (event) => {
      remoteStream = event.streams[0]
      if (isVideo) {
        nextTick(() => {
          if (remoteVideoRef.value) {
            remoteVideoRef.value.srcObject = remoteStream
          }
        })
      } else {
        remoteAudio.srcObject = remoteStream
      }
    }

    // 处理 ICE 候选
    peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        sendWebRTCMessage({
          type: 'ice-candidate',
          candidate: event.candidate
        })
      }
    }

    // 设置远程描述
    await peerConnection.setRemoteDescription(incomingCall.value.offer)

    // 创建 answer
    const answer = await peerConnection.createAnswer()
    await peerConnection.setLocalDescription(answer)

    // 发送应答
    sendWebRTCMessage({
      type: 'call-answer',
      answer: answer
    })

    isInCall.value = true
    isCallInitiator.value = false
    callStatus.value = 'connected'
    showIncomingCallDialog.value = false
    startCallTimer()

    ElMessage.success('通话已接通')
  } catch (error) {
    console.error('接听通话失败:', error)
    ElMessage.error(`接听通话失败，请检查${callType.value === 'video' ? '摄像头和麦克风' : '麦克风'}权限`)
  }
}

const rejectCall = () => {
  // 发送拒绝信令给发起通话的用户
  if (incomingCall.value) {
    const message = {
      messageType: 'webrtc',
      fromUserId: account.value.id,
      toUserId: chatUser.value.id, // 发送给当前聊天用户（发起通话的用户）
      data: {
        type: 'call-reject'
      }
    }
    console.log('发送拒绝信令给用户:', chatUser.value.id)
    socket.send(JSON.stringify(message))
  }

  showIncomingCallDialog.value = false
  incomingCall.value = null
}

const endCall = () => {
  // 保存通话记录
  saveCallRecord()

  // 发送结束通话信号
  if (isInCall.value) {
    sendWebRTCMessage({
      type: 'call-end'
    })
  }

  // 清理资源
  cleanupCall()
  ElMessage.info('通话已结束')
}

const cleanupCall = () => {
  // 停止本地流
  if (localStream) {
    localStream.getTracks().forEach(track => track.stop())
    localStream = null
  }

  // 关闭 peer connection
  if (peerConnection) {
    peerConnection.close()
    peerConnection = null
  }

  // 清理媒体元素
  if (localAudio) {
    localAudio.srcObject = null
  }
  if (remoteAudio) {
    remoteAudio.srcObject = null
  }
  if (localVideo) {
    localVideo.srcObject = null
  }
  if (remoteVideo) {
    remoteVideo.srcObject = null
  }

  // 重置状态
  isInCall.value = false
  isCallInitiator.value = false
  isMuted.value = false
  isVideoOff.value = false
  showIncomingCallDialog.value = false
  showVideoCall.value = false
  incomingCall.value = null
  callStatus.value = 'ended'
  callType.value = 'audio'

  // 停止计时器
  stopCallTimer()

  // 重置通话相关数据
  callDuration.value = 0
  callStartTime.value = null
}

const toggleMute = () => {
  if (localStream) {
    const audioTrack = localStream.getAudioTracks()[0]
    if (audioTrack) {
      audioTrack.enabled = !audioTrack.enabled
      isMuted.value = !audioTrack.enabled
    }
  }
}

const toggleVideo = () => {
  if (localStream && callType.value === 'video') {
    const videoTrack = localStream.getVideoTracks()[0]
    if (videoTrack) {
      videoTrack.enabled = !videoTrack.enabled
      isVideoOff.value = !videoTrack.enabled
    }
  }
}

const startCallTimer = () => {
  callDuration.value = 0
  callStartTime.value = new Date()
  callTimer.value = setInterval(() => {
    callDuration.value++
  }, 1000)
}

const stopCallTimer = () => {
  if (callTimer.value) {
    clearInterval(callTimer.value)
    callTimer.value = null
  }
}

const formatCallDuration = (seconds) => {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

const saveCallRecord = () => {
  // 只有成功建立连接的通话才保存记录
  if (callStatus.value === 'connected' && callDuration.value > 0) {
    const callEndTime = new Date()
    const durationText = formatCallDuration(callDuration.value)
    const callTypeText = callType.value === 'video' ? '视频' : '语音'

    const callRecord = {
      text: `${callTypeText}通话时间：${durationText}`,
      type: callType.value === 'video' ? '视频通话记录' : '通话记录',
      time: callEndTime.toLocaleString('zh-cn'),
      fromUserId: account.value.id,
      toUserId: chatUser.value.id,
      isRead: false
    }

    // 保存到数据库
    saveMessage(callRecord)

    // 添加到本地消息列表显示
    createMessage(callRecord)

    console.log('保存通话记录:', callTypeText, durationText)
  }
}

const sendWebRTCMessage = (data) => {
  const message = {
    messageType: 'webrtc',
    fromUserId: account.value.id,
    toUserId: chatUser.value.id,
    data: data
  }
  console.log('发送WebRTC信令:', data.type, 'to user:', chatUser.value.id)
  socket.send(JSON.stringify(message))
}

const handleWebRTCSignaling = async (message) => {
  const { data, fromUserId } = message

  console.log('收到WebRTC信令:', data.type, 'from user:', fromUserId)

  // 只处理当前聊天用户的信令，或者处理任何用户的来电邀请
  if (fromUserId !== chatUser.value.id && data.type !== 'call-offer') {
    console.log('忽略信令，不是当前聊天用户')
    return
  }

  try {
    switch (data.type) {
      case 'call-offer':
        // 收到通话邀请 - 需要处理任何用户的来电
        console.log('收到通话邀请，来自用户:', fromUserId, '通话类型:', data.callType)

        // 如果当前正在通话中，拒绝新的来电
        if (isInCall.value) {
          sendWebRTCMessage({
            type: 'call-reject'
          })
          return
        }

        // 设置通话类型
        callType.value = data.callType || 'audio'

        // 找到发起通话的用户信息
        const callerUser = users.value.find(user => user.id === fromUserId)
        if (callerUser) {
          // 切换到发起通话的用户
          changeUser(callerUser)
          incomingCall.value = data
          callStatus.value = 'ringing'
          showIncomingCallDialog.value = true
        }
        break

      case 'call-answer':
        // 收到通话应答
        console.log('收到通话应答')
        if (peerConnection && isCallInitiator.value) {
          await peerConnection.setRemoteDescription(data.answer)
          // 发起方收到应答后开始计时
          callStatus.value = 'connected'
          startCallTimer()
          ElMessage.success('通话已接通')
        }
        break

      case 'ice-candidate':
        // 收到 ICE 候选
        console.log('收到ICE候选')
        if (peerConnection) {
          await peerConnection.addIceCandidate(data.candidate)
        }
        break

      case 'call-reject':
        // 通话被拒绝
        console.log('通话被拒绝')
        ElMessage.warning('对方拒绝了通话')
        cleanupCall()
        break

      case 'call-end':
        // 对方结束通话
        console.log('对方结束通话')
        // 保存通话记录
        saveCallRecord()
        ElMessage.info('对方结束了通话')
        cleanupCall()
        break
    }
  } catch (error) {
    console.error('处理WebRTC信令失败:', error)
  }
}
</script>

<template>
  <div class="chat-container">
    <div class="users-panel">
      <div class="panel-header">
        <div class="header-left">
          <h3>聊天列表</h3>
        </div>
        <el-badge :value="users.length" class="user-badge" type="primary"/>
      </div>

      <div class="users-list">
        <div
            v-for="user in users"
            :key="user.id"
            class="user-item"
            :class="{ 'active': chatUser.id === user.id }"
            @click="changeUser(user)"
        >
          <div class="user-avatar">
            <img :src="user.avatarUrl" alt="用户头像">
            <span class="user-status" :class="{ 'online': user.online, 'offline': !user.online }"></span>
            <el-badge
                v-if="user.count > 0"
                :value="user.count"
                :max="99"
                class="unread-badge"
                type="danger"
            />
          </div>
          <div class="user-info">
            <div class="user-name">{{ user.nickname }}</div>
            <div class="user-status-text">
              <span :class="user.online ? 'status-online' : 'status-offline'">
                {{ user.online ? '在线' : '离线' }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="chat-panel">
      <template v-if="chatUser.id">
        <div class="chat-header">
          <div class="user-info">
            <span class="user-name">{{ chatUser.nickname }}</span>
            <span v-if="chatUser.online" class="online-status">在线</span>
          </div>
          <div class="call-controls">
            <template v-if="!isInCall">
              <el-button
                  type="primary"
                  :icon="Phone"
                  size="small"
                  @click="startCall(false)"
                  :disabled="!chatUser.online"
                  class="call-btn"
              >
                语音通话
              </el-button>
              <el-button
                  type="success"
                  :icon="VideoCamera"
                  size="small"
                  @click="startCall(true)"
                  :disabled="!chatUser.online"
                  class="call-btn"
              >
                视频通话
              </el-button>
            </template>
            <div v-else class="in-call-controls">
              <span class="call-duration">
                {{ callStatus === 'connected' ? formatCallDuration(callDuration) :
                  callStatus === 'calling' ? '呼叫中...' :
                      callStatus === 'ringing' ? '响铃中...' : '00:00' }}
              </span>
              <span class="call-type-indicator">
                {{ callType === 'video' ? '视频通话' : '语音通话' }}
              </span>
              <template v-if="callStatus === 'connected'">
                <el-button
                    :type="isMuted ? 'danger' : 'default'"
                    :icon="isMuted ? MuteNotification : Microphone"
                    size="small"
                    @click="toggleMute"
                    class="control-btn"
                >
                  {{ isMuted ? '取消静音' : '静音' }}
                </el-button>
                <el-button
                    v-if="callType === 'video'"
                    :type="isVideoOff ? 'danger' : 'default'"
                    :icon="isVideoOff ? VideoCamera : VideoCameraFilled"
                    size="small"
                    @click="toggleVideo"
                    class="control-btn"
                >
                  {{ isVideoOff ? '开启摄像头' : '关闭摄像头' }}
                </el-button>
              </template>
              <el-button
                  type="danger"
                  :icon="Close"
                  size="small"
                  @click="endCall"
                  class="end-call-btn"
              >
                结束通话
              </el-button>
            </div>
          </div>
        </div>

        <div class="messages-container" ref="messagesContainer">
          <div
              v-for="(message, index) in messages"
              :key="index"
              class="message-wrapper"
              :class="{ 'message-self': message.fromUserId === account.id }"
          >
            <div class="message-avatar" v-if="message.fromUserId !== account.id">
              <img :src="users.find(item=>item.id===message.fromUserId)?.avatarUrl" alt="头像">
            </div>

            <div class="message-content">
              <div class="message-sender" v-if="message.fromUserId !== account.id">
                {{ users.find(item => item.id === message.fromUserId)?.nickname }}
              </div>

              <div
                  v-if="message.type === '文字'"
                  class="message-bubble"
                  :class="{ 'bubble-self': message.fromUserId === account.id, 'bubble-other': message.fromUserId !== account.id }"
              >{{ message.text }}
              </div>

              <div
                  v-else-if="message.type === '通话记录' || message.type === '视频通话记录'"
                  class="message-call-record"
                  :class="{ 'call-record-self': message.fromUserId === account.id }"
              >
                <el-icon class="call-record-icon">
                  <VideoCamera v-if="message.type === '视频通话记录'"/>
                  <Phone v-else/>
                </el-icon>
                <span class="call-record-text">{{ message.text }}</span>
              </div>

              <div
                  v-else-if="message.type === '文件'"
                  class="message-file-wrapper"
                  :class="{ 'file-self': message.fromUserId === account.id }"
              >
                <el-button
                    link
                    type="primary"
                    @click="downloadFile(message.text)"
                    class="file-link"
                    :class="{ 'file-link-self': message.fromUserId === account.id }"
                >
                  <el-icon class="file-icon"><UploadFilled/></el-icon>
                  <span class="file-name">{{ getFileNameFromUrl(message.text) }}</span>
                </el-button>
              </div>

              <div
                  v-else-if="message.type === '图片'"
                  class="message-image-wrapper"
                  :class="{ 'image-self': message.fromUserId === account.id }"
              >
                <el-image
                    :src="message.text"
                    fit="cover"
                    class="message-image"
                    :preview-src-list="[message.text]"
                    preview-teleported
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon>
                        <Picture/>
                      </el-icon>
                      <span>加载失败</span>
                    </div>
                  </template>
                </el-image>
              </div>

              <div class="message-time">{{ message.time }}</div>
            </div>

            <div class="message-avatar" v-if="message.fromUserId === account.id">
              <img :src="account.avatarUrl" alt="头像">
            </div>

          </div>
        </div>

        <div class="input-container">
          <div class="message-editor">
            <div class="editor-toolbar">
              <el-upload
                  :action="`${serverHost}/web/upload`"
                  :on-success="sendImgMessage"
                  :show-file-list="false"
                  accept="image/*"
              >
                <el-button
                    type="default"
                    :icon="UploadFilled"
                    size="small"
                    class="upload-btn"
                >
                  图片
                </el-button>
              </el-upload>
              <el-upload
                  :action="`${serverHost}/web/upload`"
                  :on-success="sendFileMessage"
                  :show-file-list="false"
              >
                <el-button
                    type="default"
                    :icon="UploadFilled"
                    size="small"
                    class="upload-btn"
                >
                  文件
                </el-button>
              </el-upload>
            </div>

            <div class="textarea-wrapper">
              <div class="textarea-container">
                <el-input
                    type="textarea"
                    v-model="text"
                    :rows="1"
                    placeholder="请输入消息内容..."
                    resize="none"
                    @keydown="handleKeydown"
                    class="message-textarea"
                    @focus="clear"
                />
                <el-button
                    class="emoji-button"
                    size="small"
                    @click="toggleEmojiPanel"
                    type="text"
                >
                  😊
                </el-button>
              </div>

              <!-- 表情面板 -->
              <div v-if="showEmojiPanel" class="emoji-panel">
                <div class="emoji-categories">
                  <div
                      v-for="category in emojiCategories"
                      :key="category.key"
                      class="emoji-category-tab"
                      :class="{ 'active': activeEmojiCategory === category.key }"
                      @click="switchEmojiCategory(category.key)"
                  >
                    {{ category.name }}
                  </div>
                </div>
                <div class="emoji-list">
                  <span
                      v-for="(emoji, index) in emojiCategories.find(c => c.key === activeEmojiCategory)?.emojis"
                      :key="index"
                      class="emoji-item"
                      @click="selectEmoji(emoji)"
                  >
                    {{ emoji }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="send-actions">
            <span class="hint">
              <el-icon size="14"><Position/></el-icon>
              按 Enter 发送，Shift + Enter 换行
            </span>
            <el-button
                type="primary"
                @click="sendMessage"
                :disabled="!text || !chatUser.id"
            >
              发送消息
            </el-button>
          </div>
        </div>
      </template>

      <div class="no-chat-selected" v-else>
        <el-icon :size="64">
          <ChatRound/>
        </el-icon>
        <h3>请选择一位用户开始聊天</h3>
        <p>从左侧列表选择一位用户开始对话</p>
      </div>
    </div>

    <!-- 来电弹窗 -->
    <el-dialog
        v-model="showIncomingCallDialog"
        title="来电"
        width="400px"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
        center
        class="incoming-call-dialog"
    >
      <div class="incoming-call-content">
        <div class="caller-info">
          <img :src="chatUser.avatarUrl" alt="头像" class="caller-avatar">
          <h3>{{ chatUser.nickname }}</h3>
          <p>邀请您进行{{ callType === 'video' ? '视频' : '语音' }}通话</p>
        </div>
        <div class="call-actions">
          <el-button
              type="danger"
              :icon="Close"
              size="large"
              @click="rejectCall"
              class="reject-btn"
          >
            拒绝
          </el-button>
          <el-button
              type="success"
              :icon="callType === 'video' ? VideoCamera : Phone"
              size="large"
              @click="answerCall"
              class="answer-btn"
          >
            接听
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 视频通话界面 -->
    <el-dialog
        v-model="showVideoCall"
        title="视频通话"
        width="80%"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        :show-close="false"
        center
        class="video-call-dialog"
    >
      <div class="video-call-container">
        <div class="video-main">
          <!-- 远程视频 -->
          <div class="remote-video-container">
            <video
                ref="remoteVideoRef"
                class="remote-video"
                autoplay
                playsinline
            ></video>
            <div v-if="!remoteStream" class="video-placeholder">
              <el-icon :size="64"><VideoCamera/></el-icon>
              <p>等待对方开启摄像头...</p>
            </div>
          </div>

          <!-- 本地视频 -->
          <div class="local-video-container">
            <video
                ref="localVideoRef"
                class="local-video"
                autoplay
                playsinline
                muted
            ></video>
            <div v-if="isVideoOff" class="video-off-overlay">
              <el-icon :size="32"><VideoCamera/></el-icon>
              <p>摄像头已关闭</p>
            </div>
          </div>
        </div>

        <!-- 视频通话控制栏 -->
        <div class="video-call-controls">
          <div class="call-info">
            <span class="caller-name">{{ chatUser.nickname }}</span>
            <span class="call-duration">{{ formatCallDuration(callDuration) }}</span>
          </div>

          <div class="control-buttons">
            <el-button
                :type="isMuted ? 'danger' : 'default'"
                :icon="isMuted ? MuteNotification : Microphone"
                size="large"
                @click="toggleMute"
                class="video-control-btn"
                circle
            />
            <el-button
                :type="isVideoOff ? 'danger' : 'default'"
                :icon="isVideoOff ? VideoCamera : VideoCameraFilled"
                size="large"
                @click="toggleVideo"
                class="video-control-btn"
                circle
            />
            <el-button
                type="danger"
                :icon="Close"
                size="large"
                @click="endCall"
                class="video-control-btn end-call"
                circle
            />
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.chat-container {
  display: flex;
  height: calc(90vh - 90px);
  min-height: 600px;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  margin: 20px auto;
  max-width: 1200px;
}

.users-panel {
  width: 280px;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  background-color: #fafafa;
}

.panel-header {
  height: 60px;
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-button {
  padding: 0;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.user-badge {
  margin-top: 2px;
}

.users-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.user-item {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 6px;
  position: relative;
}

.user-item:hover {
  background-color: #f5f5f5;
}

.user-item.active {
  background-color: #f0f7ff;
}

.user-avatar {
  position: relative;
  margin-right: 12px;
}

.user-avatar img {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid #fff;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
}

.unread-badge :deep(.el-badge__content) {
  border: 2px solid #fff;
  font-size: 11px;
  height: 18px;
  line-height: 14px;
  padding: 0 5px;
  font-weight: 600;
}

.user-status {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #fff;
  transition: background-color 0.3s ease;
}

.user-status.online {
  background-color: #52c41a; /* 绿色表示在线 */
}

.user-status.offline {
  background-color: #d9d9d9; /* 灰色表示离线 */
}

.user-info {
  flex: 1;
  overflow: hidden;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.user-status-text {
  font-size: 12px;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
}

.chat-header {
  height: 60px;
  padding: 18px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.online-status {
  font-size: 12px;
  color: #52c41a;
  background-color: #f6ffed;
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid #b7eb8f;
}

.call-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.call-btn {
  border-radius: 20px;
  padding: 8px 16px;
  margin-right: 8px;
}

.call-btn:last-child {
  margin-right: 0;
}

.in-call-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background-color: #f0f7ff;
  border-radius: 20px;
  border: 1px solid #91d5ff;
}

.call-duration {
  font-size: 14px;
  font-weight: 600;
  color: #1890ff;
  min-width: 50px;
}

.call-type-indicator {
  font-size: 12px;
  color: #666;
  background-color: #f5f5f5;
  padding: 2px 8px;
  border-radius: 10px;
}

.control-btn {
  border-radius: 16px;
  padding: 6px 12px;
  margin: 0 4px;
}

.end-call-btn {
  border-radius: 16px;
  padding: 6px 12px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f9fafc;
  background-image: linear-gradient(rgba(240, 240, 240, 0.5) 1px, transparent 1px),
  linear-gradient(90deg, rgba(240, 240, 240, 0.5) 1px, transparent 1px);
  background-size: 20px 20px;
}

.message-wrapper {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;
  gap: 12px;
}

.message-wrapper.message-self {
  justify-content: flex-end;
}

.message-avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border: 2px solid #fff;
  flex-shrink: 0;
}

.message-content {
  max-width: 60%;
  display: flex;
  flex-direction: column;
}

.message-wrapper:not(.message-self) .message-content {
  align-items: flex-start;
}

.message-wrapper.message-self .message-content {
  align-items: flex-end;
}

.message-sender {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
  padding: 0 4px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  position: relative;
  max-width: 100%;
}

.message-bubble.bubble-other {
  background-color: #fff;
  color: #333;
  border-top-left-radius: 4px;
}

.message-bubble.bubble-self {
  background-color: #1890ff;
  color: #fff;
  border-top-right-radius: 4px;
}

.message-image-wrapper {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: #f5f5f5;
}

.message-image-wrapper.image-self {
  border-top-right-radius: 4px;
}

.message-image-wrapper:not(.image-self) {
  border-top-left-radius: 4px;
}

.message-image {
  width: 200px;
  height: 200px;
  display: block;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.message-image:hover {
  transform: scale(1.02);
}

.message-image :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-error {
  width: 200px;
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f5f5f5;
  color: #999;
  gap: 8px;
}

.image-error .el-icon {
  font-size: 32px;
}

.message-time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  padding: 0 4px;
}

.message-call-record {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  background-color: #f0f9ff;
  border: 1px solid #bae6fd;
  color: #0369a1;
  max-width: 200px;
}

.message-call-record.call-record-self {
  background-color: #ecfdf5;
  border-color: #bbf7d0;
  color: #059669;
}

.call-record-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.call-record-text {
  font-weight: 500;
}

.input-container {
  padding: 15px 20px;
  border-top: 1px solid #f0f0f0;
  background-color: #fff;
}

.message-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.textarea-wrapper {
  position: relative;
}

.textarea-container {
  position: relative;
  display: flex;
  align-items: stretch;
}

.editor-toolbar {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  background-color: #fafafa;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

.upload-btn {
  border-radius: 6px;
  font-size: 13px;
  height: 32px;
  padding: 0 16px;
  transition: all 0.3s ease;
  border-color: #d9d9d9;
}

.upload-btn:hover {
  color: #1890ff;
  border-color: #1890ff;
  background-color: #f0f7ff;
}

.message-file-wrapper {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  background-color: #fff;
  border: 1px solid #f0f0f0;
}

.message-file-wrapper.file-self {
  background-color: #1890ff;
  border-color: #1890ff;
  border-top-right-radius: 4px;
}

.message-file-wrapper:not(.file-self) {
  border-top-left-radius: 4px;
}

.file-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: #1890ff;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 0;
  border: none;
  background: none;
  height: auto;
  font-size: 14px;
}

.message-file-wrapper.file-self .file-link,
.file-link-self {
  color: #fff;
}

.file-link:hover {
  opacity: 0.8;
}

.file-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.file-name {
  max-width: 200px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
}

.message-textarea {
  flex: 1;
}

.message-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  border-color: #e8e8e8;
  padding: 12px 40px 12px 12px; /* 右侧留出空间给表情按钮 */
  transition: all 0.3s;
  font-size: 14px;
  line-height: 1.6;
}

.message-textarea :deep(.el-textarea__inner):focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.emoji-button {
  position: absolute;
  right: 8px;
  bottom: 8px;
  z-index: 10;
  transition: all 0.3s ease;
  border: none;
  background: transparent;
  padding: 4px;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.emoji-button:hover {
  background-color: #f0f7ff;
  transform: scale(1.1);
}

.emoji-button:focus {
  outline: none;
  box-shadow: none;
}

.emoji-panel {
  position: absolute;
  bottom: 100%;
  right: 0;
  width: 500px;
  height: 320px;
  background: white;
  border: 1px solid #e8e8e8;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  margin-bottom: 8px;
}

.emoji-categories {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
  border-radius: 12px 12px 0 0;
  flex-wrap: wrap;
}

.emoji-category-tab {
  padding: 8px 10px;
  font-size: 11px;
  color: #666;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.3s ease;
  border-bottom: 2px solid transparent;
  flex: 1;
  text-align: center;
  min-width: 0;
}

.emoji-category-tab:hover {
  color: #1890ff;
  background-color: #f0f7ff;
}

.emoji-category-tab.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
  background-color: #f0f7ff;
}

.emoji-list {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  overflow-x: hidden;
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 10px;
  align-content: start;
}

.emoji-item {
  font-size: 24px;
  padding: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: center;
  user-select: none;
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 1;
}

.emoji-item:hover {
  background-color: #f0f7ff;
  transform: scale(1.2);
}

.emoji-list::-webkit-scrollbar {
  width: 6px;
}

.emoji-list::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.emoji-list::-webkit-scrollbar-track {
  background-color: transparent;
}

.send-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.no-chat-selected {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  padding: 20px;
  text-align: center;
  background-color: #fafafa;
}

.no-chat-selected .el-icon {
  margin-bottom: 20px;
  color: #d9d9d9;
}

.no-chat-selected h3 {
  margin: 0 0 10px;
  font-size: 18px;
  font-weight: 500;
  color: #333;
}

.no-chat-selected p {
  margin: 0;
  font-size: 14px;
}

.users-list::-webkit-scrollbar,
.messages-container::-webkit-scrollbar {
  width: 6px;
}

.users-list::-webkit-scrollbar-thumb,
.messages-container::-webkit-scrollbar-thumb {
  background-color: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.users-list::-webkit-scrollbar-track,
.messages-container::-webkit-scrollbar-track {
  background-color: transparent;
}

/* 来电弹窗样式 */
.incoming-call-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}

.incoming-call-dialog :deep(.el-dialog__header) {
  background-color: #1890ff;
  color: white;
  padding: 20px;
  text-align: center;
}

.incoming-call-dialog :deep(.el-dialog__title) {
  color: white;
  font-size: 18px;
  font-weight: 600;
}

.incoming-call-content {
  padding: 20px;
  text-align: center;
}

.caller-info {
  margin-bottom: 30px;
}

.caller-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.caller-info h3 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.caller-info p {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.call-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.reject-btn,
.answer-btn {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  font-size: 24px;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s ease;
}

.reject-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(245, 34, 45, 0.3);
}

.answer-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(82, 196, 26, 0.3);
}

/* 视频通话界面样式 */
.video-call-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  max-width: 1000px;
}

.video-call-dialog :deep(.el-dialog__header) {
  background-color: #1890ff;
  color: white;
  padding: 15px 20px;
}

.video-call-dialog :deep(.el-dialog__title) {
  color: white;
  font-size: 16px;
  font-weight: 600;
}

.video-call-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.video-call-container {
  position: relative;
  background-color: #000;
  min-height: 500px;
}

.video-main {
  position: relative;
  width: 100%;
  height: 500px;
}

.remote-video-container {
  position: relative;
  width: 100%;
  height: 100%;
  background-color: #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remote-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.local-video-container {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 200px;
  height: 150px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  background-color: #2a2a2a;
}

.local-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  text-align: center;
}

.video-placeholder .el-icon {
  margin-bottom: 16px;
  color: #666;
}

.video-placeholder p {
  margin: 0;
  font-size: 14px;
}

.video-off-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.video-off-overlay .el-icon {
  margin-bottom: 8px;
}

.video-off-overlay p {
  margin: 0;
  font-size: 12px;
}

.video-call-controls {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.call-info {
  display: flex;
  flex-direction: column;
  color: white;
}

.caller-name {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 4px;
}

.call-info .call-duration {
  font-size: 14px;
  color: #ccc;
  min-width: auto;
}

.control-buttons {
  display: flex;
  gap: 16px;
  align-items: center;
}

.video-control-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  font-size: 20px;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.video-control-btn:hover {
  transform: scale(1.1);
}

.video-control-btn.end-call {
  background-color: #ff4d4f;
  color: white;
}

.video-control-btn.end-call:hover {
  background-color: #ff7875;
  box-shadow: 0 6px 16px rgba(255, 77, 79, 0.4);
}
</style>
