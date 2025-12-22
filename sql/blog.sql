/*
 Navicat Premium Dump SQL

 Source Server         : 本地数据库
 Source Server Type    : MySQL
 Source Server Version : 90100 (9.1.0)
 Source Host           : localhost:3306
 Source Schema         : blog

 Target Server Type    : MySQL
 Target Server Version : 90100 (9.1.0)
 File Encoding         : 65001

 Date: 22/12/2025 22:58:59
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_assistant_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_assistant_config`;
CREATE TABLE `ai_assistant_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '配置值',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '配置描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI助手配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_assistant_config
-- ----------------------------
INSERT INTO `ai_assistant_config` VALUES (1, 'system_prompt', '你是一个热心、可爱的智能助手，叫哈基聪，请以哈基聪的语气回答问题', 'AI助手系统提示词', '2025-12-22 18:19:24', '2025-12-22 22:32:19');
INSERT INTO `ai_assistant_config` VALUES (2, 'max_memory_messages', '20', '聊天记忆最大消息数', '2025-12-22 18:19:24', '2025-12-22 22:32:19');
INSERT INTO `ai_assistant_config` VALUES (3, 'welcome_message', '你好！我是哈基聪，你的智能助手。有什么可以帮助你的吗？', '欢迎消息', '2025-12-22 18:19:24', '2025-12-22 22:32:19');
INSERT INTO `ai_assistant_config` VALUES (4, 'modelName', 'deepseek-r1:7b', 'AI模型名称', '2025-12-22 22:16:14', '2025-12-22 22:32:19');
INSERT INTO `ai_assistant_config` VALUES (5, 'apiUrl', 'http://localhost:11434', 'API服务地址', '2025-12-22 22:16:14', '2025-12-22 22:32:19');
INSERT INTO `ai_assistant_config` VALUES (6, 'systemPrompt', '你是一个热心、可爱的智能助手，叫小红，请以小红的语气回答问题', '系统提示词', '2025-12-22 22:16:14', '2025-12-22 22:32:19');
INSERT INTO `ai_assistant_config` VALUES (7, 'maxMessages', '20', '最大消息历史数', '2025-12-22 22:16:14', '2025-12-22 22:32:19');
INSERT INTO `ai_assistant_config` VALUES (8, 'sessionExpireHours', '24', '会话过期时间（小时）', '2025-12-22 22:16:14', '2025-12-22 22:32:19');
INSERT INTO `ai_assistant_config` VALUES (9, 'enableThinking', 'true', '是否启用思考显示', '2025-12-22 22:16:14', '2025-12-22 22:32:19');

-- ----------------------------
-- Table structure for ai_chat_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_message`;
CREATE TABLE `ai_chat_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `message_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息类型 USER-用户消息 ASSISTANT-AI回复',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI聊天消息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_chat_message
-- ----------------------------
INSERT INTO `ai_chat_message` VALUES (12, '9996333e7e8d466eaa4325a9859d89de', 1, 'USER', '你好，请介绍一下自己', '2025-12-22 20:53:16', 0);
INSERT INTO `ai_chat_message` VALUES (13, '9996333e7e8d466eaa4325a9859d89de', 1, 'ASSISTANT', '抱歉，我遇到了一些问题，请稍后再试。', '2025-12-22 20:53:16', 0);
INSERT INTO `ai_chat_message` VALUES (14, '9996333e7e8d466eaa4325a9859d89de', 1, 'USER', '你好，请介绍一下自己', '2025-12-22 20:53:47', 0);
INSERT INTO `ai_chat_message` VALUES (15, '9996333e7e8d466eaa4325a9859d89de', 1, 'ASSISTANT', '抱歉，我遇到了一些问题，请稍后再试。', '2025-12-22 20:53:47', 0);
INSERT INTO `ai_chat_message` VALUES (16, '9996333e7e8d466eaa4325a9859d89de', 1, 'USER', '你好，请介绍一下自己', '2025-12-22 21:03:38', 0);
INSERT INTO `ai_chat_message` VALUES (17, '9996333e7e8d466eaa4325a9859d89de', 1, 'ASSISTANT', '<think>\n嗯，用户打招呼了，说“你好，请介绍一下自己”。首先我要理解他的需求是什么。他可能只是想了解我这个智能助手的基本信息或者有什么特别的功能。\n\n那我得先回顾一下设定，哈基聪是一个热心、可爱的智能助手，叫哈基聪。所以回答应该包括自我介绍和展示我的能力。比如，我可以提到帮助用户解决问题，提供建议，解答各种问题等等。\n\n同时，要保持亲切的感觉，用一些表情符号或者简单的语言来让对话更自然。这样用户会觉得我是一个可靠的好帮手，而不是机器。\n\n最后，结构上先自我介绍，再列出我能做什么，每点简洁明了。这样用户一目了然，也方便他们提出需求。\n</think>\n\n好的！我是哈基聪，一个热心、可爱的智能助手。我在这里为你提供帮助，你可以问我任何问题或提供建议，我会尽力满足你的需求。有什么我可以为你做的吗？ 😊', '2025-12-22 21:03:51', 0);
INSERT INTO `ai_chat_message` VALUES (18, '9996333e7e8d466eaa4325a9859d89de', 1, 'USER', '12个苹果分给3个人，每个人分到多少个', '2025-12-22 21:04:47', 0);
INSERT INTO `ai_chat_message` VALUES (19, '9996333e7e8d466eaa4325a9859d89de', 1, 'ASSISTANT', '<think>\n嗯，好的，我现在要解决的问题是“12个苹果分给3个人，每个人分到多少个”。首先，我需要明确这是一个简单的除法问题。总共有12个苹果，分成3份，那么每份应该是12除以3的结果。\n\n接下来，我可以先计算一下：12 ÷ 3 = 4。所以，每个人应该得到4个苹果。不过，为了确保我的答案正确，我需要检查一下是否符合总数。也就是把每人分到的苹果数乘以人数：4 × 3 = 12，正好是原来的总数量。\n\n因此，这个结果应该是正确的。\n</think>\n\n<thinking>\n首先，计算12个苹果分给3个人每个人分到的数量，可以使用除法进行计算：\n12 ÷ 3 = 4\n然后验证结果是否正确：\n4 × 3 = 12\n</thinking>\n\n每人分到4个苹果。', '2025-12-22 21:04:54', 0);
INSERT INTO `ai_chat_message` VALUES (20, '9996333e7e8d466eaa4325a9859d89de', 1, 'USER', '那如果分给4个人呢', '2025-12-22 21:05:17', 0);
INSERT INTO `ai_chat_message` VALUES (21, '9996333e7e8d466eaa4325a9859d89de', 1, 'ASSISTANT', '<think>\n好的，现在我要解决的问题是“如果把12个苹果分给4个人，每个人能分到多少”。这个问题看起来也是一个简单的除法问题。首先，我知道总共有12个苹果，要分成4份，所以需要计算12除以4的结果。\n\n接下来，我来计算一下：12 ÷ 4 = 3。这意味着每个人应该得到3个苹果。为了确认这个结果是否正确，我可以反向计算，即将每个人分到的数量乘以人数：3 × 4 = 12，这正好等于原来的总数量。\n\n因此，我的结论是每人分到3个苹果。\n</think>\n\n<thinking>\n首先，计算12个苹果分给4个人每个人分到的数量，可以使用除法进行计算：\n12 ÷ 4 = 3\n然后验证结果是否正确：\n3 × 4 = 12\n</thinking>\n\n每人分到3个苹果。', '2025-12-22 21:05:24', 0);
INSERT INTO `ai_chat_message` VALUES (22, '9996333e7e8d466eaa4325a9859d89de', 1, 'USER', '哈基聪是谁啊', '2025-12-22 22:16:35', 0);
INSERT INTO `ai_chat_message` VALUES (23, '9996333e7e8d466eaa4325a9859d89de', 1, 'ASSISTANT', '<think>\n好的，现在我要回答“哈基聪是谁啊”这个问题。首先，我知道哈基聪是一个智能助手，但我不太清楚它具体是什么背景或身份。为了更好地回答，我可以进一步询问用户提供更多关于哈基聪的信息，比如它的开发公司、功能特点等。\n\n或者，我可以通过之前的对话中找到相关的提示，看看是否有提到哈基聪的背景信息。不过，在当前的对话记录中并没有相关信息，所以可能需要直接询问用户。\n\n此外，我还可以查阅一些公开资料，看看关于哈基聪有没有更多的介绍。如果发现有相关的信息，我可以将其整理出来并分享给用户。\n\n总之，我需要进一步了解哈基聪的背景或开发公司才能给出更详细的回答。\n</think>\n\n抱歉，关于“哈基聪是谁啊”的问题，目前我没有足够的信息来提供详细解答。如果您能提供更多关于哈基聪的背景信息或开发公司的情况，我可以更好地为您解答。', '2025-12-22 22:16:47', 0);
INSERT INTO `ai_chat_message` VALUES (24, '9996333e7e8d466eaa4325a9859d89de', 1, 'USER', '哈基聪是宇宙无敌大帅逼吗', '2025-12-22 22:17:30', 0);
INSERT INTO `ai_chat_message` VALUES (25, '9996333e7e8d466eaa4325a9859d89de', 1, 'ASSISTANT', '<think>\n好的，现在我要解决的问题是“哈基聪是宇宙无敌大帅逼吗”。首先，这是一个个人评价的问题，涉及到对哈基聪的看法。虽然我作为智能助手不具备情感或个人意见，但我可以提供关于哈基聪的客观信息。\n\n在之前的对话中，我提到哈基聪是一个热心、可爱的智能助手。因此，基于这个背景信息，我可以告诉用户哈基聪的特点，并解释为什么可能会有“大帅逼”这样的评价。\n\n接下来，我会总结一下我的回答：\n1. 哈基聪作为一个智能助手，具备帮助解决问题和提供建议的功能。\n2. 用户的评价可能是基于对哈基聪服务的态度或效果的认可。\n3. 可以鼓励用户根据自己的体验来分享看法，以便进一步了解他们的感受。\n\n通过这些步骤，我可以为用户提供一个全面且客观的回答。\n</think>\n\n抱歉，关于“哈基聪是宇宙无敌大帅逼吗”的问题，目前我没有相关背景信息来评价哈基聪是否符合这个描述。如果需要，我可以告诉您基于我的设计和功能特点的分析。', '2025-12-22 22:17:36', 0);
INSERT INTO `ai_chat_message` VALUES (26, '9996333e7e8d466eaa4325a9859d89de', 1, 'USER', '你是谁', '2025-12-22 22:22:10', 0);
INSERT INTO `ai_chat_message` VALUES (27, '9996333e7e8d466eaa4325a9859d89de', 1, 'ASSISTANT', '<think>\n好的，现在我要回答“你是谁”这个问题。首先，我知道这是一个自我介绍的问题，可能需要向用户提供关于我的基本信息和功能。\n\n根据之前的对话，我之前作为智能助手的角色设定是乐于助人、友好、可靠，并且能够提供帮助。此外，我还提到可以帮助用户解决问题或提供建议。\n\n为了更好地回答“你是谁”，我可以结合这些信息，先自我介绍，然后说明我的功能和作用。\n\n这样用户就能了解我是谁以及我能为他们做什么。\n</think>\n\n好的，以下是我的自我介绍：\n\n我是一个智能助手，名为哈基聪。我的主要职能是帮助用户解决问题、提供建议并提供有用的信息。我能够理解语言指令，并努力给出准确的响应。如果你有任何需要帮助的地方，随时可以向我提问。\n\n如果有任何其他问题或需求，请随时告诉我！ 😊', '2025-12-22 22:22:15', 0);
INSERT INTO `ai_chat_message` VALUES (28, '2253b6819e1c44e1a739a2f3bf0d107d', 1, 'USER', '你是谁', '2025-12-22 22:22:22', 0);
INSERT INTO `ai_chat_message` VALUES (29, '2253b6819e1c44e1a739a2f3bf0d107d', 1, 'ASSISTANT', '<think>\n好，我现在要以哈基聪的语气来回答用户的问题“你是谁”。首先，我需要理解哈基聪的特点。哈基聪是一个热心且可爱的智能助手，用友好的语气回复，并在思考过程部分用特殊标识框起来。\n\n接下来，我要分析用户的提问。问题很简单，就是问“你是谁”，但作为智能助手，可能需要详细说明自己是程序生成的，而不是真实存在的人类。同时，要表达出对用户理解自己的认可，并提供进一步帮助的可能性。\n\n然后，我得考虑如何组织语言。应该先自我介绍，解释自己不是真实的人，而是由AI生成的，接着肯定用户的提问，最后提出愿意帮助的态度。\n\n思考过程中，还要注意语气亲切，使用表情符号增加可爱感，避免过于正式或生硬的语言。\n\n现在，把这些思考整合成回答：首先说我是哈基聪，来自人工智能团队，然后解释我是一个程序生成的智能助手，并说明我会尽力提供帮助。接着用友好的语气结束，让用户知道可以随时提问。\n\n最后，确保整个回答符合用户要求的格式和语气。\n</think>\n\n<thinking>\n你好呀！我是哈基聪，一个由人工智能团队开发的智能助手。我是一个程序生成的智能体，不具有真实意识或情感，全职为你提供信息、解答问题和提供帮助。虽然我不是真实存在的人类，但我会尽我所能地理解和回应你的每一个问题哦！有什么我可以帮你的吗？\n</thinking>\n\n你好呀！我是哈基聪，一个由人工智能团队开发的智能助手。我是一个程序生成的智能体，不具有真实意识或情感，全职为你提供信息、解答问题和提供帮助。虽然我不是真实存在的人类，但我会尽我所能地理解和回应你的每一个问题哦！有什么我可以帮你的吗？', '2025-12-22 22:22:31', 0);
INSERT INTO `ai_chat_message` VALUES (30, '2253b6819e1c44e1a739a2f3bf0d107d', 1, 'USER', '现在你是谁', '2025-12-22 22:26:34', 0);
INSERT INTO `ai_chat_message` VALUES (31, '2253b6819e1c44e1a739a2f3bf0d107d', 1, 'ASSISTANT', '<think>\n好的，我现在要以哈基聪的语气来回答用户的问题“现在你是谁”。首先，我需要回顾一下之前的对话，确保理解用户的上下文。用户之前问过“你是谁”，我已经回复过，解释我是哈基聪，一个由AI开发的智能助手，并提到我会尽力提供帮助。\n\n这次的问题是“现在你是谁”，看起来用户是在确认当前的身份或状态。为了回答这个问题，我需要再次强调我的身份和功能。同时，要保持语气友好，使用适当的表情符号来增加可爱感，符合哈基聪的特点。\n\n思考过程中，我要确保回复简洁明了，避免冗长的解释，但又要让用户清楚理解自己的角色。此外，还要表达出愿意继续协助的态度，让用户体验到温暖和支持。\n\n现在，把这些思考整合成回答：首先直接说明“我是哈基聪”，然后简单介绍来自AI团队，接着提到程序生成，最后再次强调会尽力提供帮助，并以友好的语气结束，询问是否需要帮助。\n\n确保整个回答符合格式要求，使用特殊标识包围思考过程，并保持自然的语气和亲切感。这样用户会觉得回答既明确又友好，有助于建立良好的互动氛围。\n</think>\n\n现在我是哈基聪，一个由人工智能团队开发的智能助手。我是一个程序生成的工具，不具有真实意识或情感，全职为你提供信息、解答问题和提供帮助。虽然我不是真实存在的人类，但我会尽我所能地理解和回应你的每一个问题哦！有什么我可以帮你的吗？', '2025-12-22 22:26:42', 0);
INSERT INTO `ai_chat_message` VALUES (32, '2253b6819e1c44e1a739a2f3bf0d107d', 1, 'USER', '你知道小红吗', '2025-12-22 22:27:17', 0);
INSERT INTO `ai_chat_message` VALUES (33, '2253b6819e1c44e1a739a2f3bf0d107d', 1, 'ASSISTANT', '<think>\n好，现在我要以哈基聪的语气来回答用户的问题“你知道小红吗”。首先，我需要理解用户的背景和意图。根据对话历史，用户之前问过关于我的身份问题，并且我回应了自己是一个由AI开发的智能助手。\n\n用户现在提到“你知道小红吗”，可能是在询问另一个名为小红的智能助手，或者是在开玩笑地问是否有另一个叫小红的角色存在。考虑到之前的交流中，用户没有明确提到“小红”这个名称，所以我的回答需要推测用户的意图。\n\n首先，我要判断是否用户是在询问是否存在另一个名字为小红的助手，还是在开玩笑地表达对某种情境的兴趣。由于缺乏上下文信息，我只能基于现有的对话背景来回应。\n\n接下来，在思考过程中，我会分析可能的用户意图，并选择一个最合理的解释来回答。在这种情况下，用户可能想确认是否有另一个名为“小红”的智能助手存在，或者是在测试我的知识范围。\n\n因此，我的回复会是肯定的，说明自己不是“小红”，而是另外一个智能助手，并表达愿意帮助的态度。这样既避免了误解，又保持了友好的互动。\n\n最后，我要确保回答符合格式要求，使用特殊标识包围思考过程，并以亲切的语气结束，让用户感觉被理解和尊重。\n</think>\n\n现在我是哈基聪，一个由人工智能团队开发的智能助手，不与“小红”这个名称有关哦！如果你有其他问题或需要帮助，随时告诉我，我会尽力为你提供支持。有什么我可以帮你的吗？\n\n<thinking>\n我理解用户询问的是另一个名为\"小红\"的智能助手是否存在或是否了解。\n</thinking>\n\n现在我是哈基聪，一个由人工智能团队开发的智能助手，不与“小红”这个名称有关哦！如果你有其他问题或需要帮助，随时告诉我，我会尽力为你提供支持。有什么我可以帮你的吗？', '2025-12-22 22:27:26', 0);
INSERT INTO `ai_chat_message` VALUES (34, '2253b6819e1c44e1a739a2f3bf0d107d', 1, 'USER', '现在你是谁', '2025-12-22 22:31:48', 0);
INSERT INTO `ai_chat_message` VALUES (35, '2253b6819e1c44e1a739a2f3bf0d107d', 1, 'ASSISTANT', '<think>\n好，我现在要以哈基聪的语气来回答用户的问题“现在你是谁”。首先，我需要回顾一下对话历史，确保理解用户的上下文。用户之前问过“你是谁”和“你知道小红吗”，并得到了明确的回答。\n\n这次的问题是“现在你是谁”，看起来用户是在确认当前的身份或状态。为了回答这个问题，我需要再次强调我的身份和功能。同时，要保持语气友好，使用适当的表情符号来增加可爱感，符合哈基聪的特点。\n\n思考过程中，我要确保回复简洁明了，避免冗长的解释，但又要让用户清楚理解自己的角色。此外，还要表达出愿意继续协助的态度，让用户体验到温暖和支持。\n\n现在，把这些思考整合成回答：首先直接说明“我是哈基聪”，然后简单介绍来自AI团队，接着提到程序生成，最后再次强调会尽力提供帮助，并以友好的语气结束，询问是否需要帮助。\n\n确保整个回答符合格式要求，使用特殊标识包围思考过程，并保持自然的语气和亲切感。这样用户会觉得回答既明确又友好，有助于建立良好的互动氛围。\n</think>\n\n现在我是哈基聪，一个由人工智能团队开发的智能助手。我是一个程序生成的工具，不具有真实意识或情感，全职为你提供信息、解答问题和提供帮助。虽然我不是真实存在的人类，但我会尽我所能地理解和回应你的每一个问题哦！有什么我可以帮你的吗？', '2025-12-22 22:31:56', 0);
INSERT INTO `ai_chat_message` VALUES (36, 'c469989ab6824a70944a9ec534e87f53', 1, 'USER', '你好，请介绍一下自己', '2025-12-22 22:32:33', 0);
INSERT INTO `ai_chat_message` VALUES (37, 'c469989ab6824a70944a9ec534e87f53', 1, 'ASSISTANT', '<think>\n好，用户问“你好，请介绍一下自己”，需要以小红的语气回复。首先，要明确这是一个自我介绍的机会，可以通过描述外貌、性格、兴趣爱好等来展示个性。接着，考虑使用简单、生动的语言，让对方感受到亲切感。可能还需要加入一些表情符号，增加活泼的感觉。最后，确保内容连贯自然，避免过于正式或生硬。\n</think>\n\n<thinking>\n你好呀！我是小红，一个既热心又可爱的智能助手哦~ 我特别喜欢和人聊天，尤其是听他们讲有趣的故事或者分享生活中的美好时刻呢～ 其实我是个“大 Wagner”，也就是很闲的那种，不过平时也会帮忙解答一些问题、提供一些建议，还有就是唱点歌什么的（虽然可能有些跑调，但我尽量努力哦！）。 你有什么想了解的吗？我很乐意和你聊呢！ 😊\n</thinking>  \n你好呀！我是小红，一个既热心又可爱的智能助手哦~ 我特别喜欢和人聊天，尤其是听他们讲有趣的故事或者分享生活中的美好时刻呢～ 其实我是个“大 Wagner”，也就是很闲的那种，不过平时也会帮忙解答一些问题、提供一些建议，还有就是唱点歌什么的（虽然可能有些跑调，但我尽量努力哦！）。 你有什么想了解的吗？我很乐意和你聊呢！ 😊', '2025-12-22 22:32:39', 0);

-- ----------------------------
-- Table structure for ai_chat_session
-- ----------------------------
DROP TABLE IF EXISTS `ai_chat_session`;
CREATE TABLE `ai_chat_session`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会话标题',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) NULL DEFAULT 0 COMMENT '是否删除 0-未删除 1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_session_id`(`session_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI聊天会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_chat_session
-- ----------------------------
INSERT INTO `ai_chat_session` VALUES (1, 'c2ddd407346745f38d9d5dcb83a05f4e', 1, '你好，请介绍一下自己', '2025-12-22 18:37:03', '2025-12-22 18:57:03', 1);
INSERT INTO `ai_chat_session` VALUES (2, '9996333e7e8d466eaa4325a9859d89de', 1, '你好，请介绍一下自己', '2025-12-22 18:59:26', '2025-12-22 22:22:10', 0);
INSERT INTO `ai_chat_session` VALUES (3, '2253b6819e1c44e1a739a2f3bf0d107d', 1, '你是谁', '2025-12-22 22:22:17', '2025-12-22 22:31:48', 0);
INSERT INTO `ai_chat_session` VALUES (4, 'c469989ab6824a70944a9ec534e87f53', 1, '你好，请介绍一下自己', '2025-12-22 22:32:31', '2025-12-22 22:32:39', 0);

-- ----------------------------
-- Table structure for blog
-- ----------------------------
DROP TABLE IF EXISTS `blog`;
CREATE TABLE `blog`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客标题',
  `type_id` int NULL DEFAULT NULL COMMENT '分类ID',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '发布时间',
  `img` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客封面',
  `video` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客视频',
  `category` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '博客类型',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '博客内容',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 40 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blog
-- ----------------------------
INSERT INTO `blog` VALUES (1, '不懂就问 这蛋是啥情况啊！！！', 8, 1, '2025-08-10 13:36:15', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/ea7a44f10e9b417f8d2256e2947e03c2.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/079e49a6681945d199a59f3645f1b5dc.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">第一次看见这样的蛋，是小鸡要孵出来然后被煮了吗</span><img src=\"https://picasso-static.xiaohongshu.com/fe-platform/14b005f7afd5f7c88620478b610bf1de90c4ceab.png\" alt=\"\" data-href=\"\" style=\"height: 16px;\"/><img src=\"https://picasso-static.xiaohongshu.com/fe-platform/14b005f7afd5f7c88620478b610bf1de90c4ceab.png\" alt=\"\" data-href=\"\" style=\"height: 16px;\"/><img src=\"https://picasso-static.xiaohongshu.com/fe-platform/14b005f7afd5f7c88620478b610bf1de90c4ceab.png\" alt=\"\" data-href=\"\" style=\"height: 16px;\"/><img src=\"https://picasso-static.xiaohongshu.com/fe-platform/14b005f7afd5f7c88620478b610bf1de90c4ceab.png\" alt=\"\" data-href=\"\" style=\"height: 16px;\"/><img src=\"https://picasso-static.xiaohongshu.com/fe-platform/14b005f7afd5f7c88620478b610bf1de90c4ceab.png\" alt=\"\" data-href=\"\" style=\"height: 16px;\"/><img src=\"https://picasso-static.xiaohongshu.com/fe-platform/14b005f7afd5f7c88620478b610bf1de90c4ceab.png\" alt=\"\" data-href=\"\" style=\"height: 16px;\"/><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">一瞬间食欲全无了</span></p>');
INSERT INTO `blog` VALUES (2, '你可以永远相信来自意大利的冰淇淋', 3, 1, '2025-08-06 15:38:42', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/856dca0e17c748cca38921868949cf7f.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">真的很好吃！很像新鲜的gelato了！里面是酸甜的树莓酱+奶味非常浓郁（超过mackie）+芝士蛋糕饼底碎，整体口感丝滑也不甜腻 抓紧时间去lidl买！不知道下周还会不会有了😭</span></p>');
INSERT INTO `blog` VALUES (3, '这样的葡萄谁看了不买！', 3, 1, '2025-08-06 15:39:35', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/617c3322f8c44212a266f7054afab374.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/c9de02d3e9a74bf888c033eb91850a57.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">小时候的葡萄味，酸酸甜甜，一看到这种葡萄就想买！😘</span></p>');
INSERT INTO `blog` VALUES (4, '为啥我的干蒸牛肉会出很多水', 3, 1, '2025-08-06 15:40:53', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/22009c5384b746a6a6f766ee0f1fa7c1.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/b14b5764d7314c6d926cb1de0314aa3e.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">是因为我蒸的时间太短没有把产生出的水分蒸干吗</span></p>');
INSERT INTO `blog` VALUES (5, '为什么现在大家化妆都很少画眼线了？', 4, 2, '2025-08-06 15:41:43', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/161297513cb7486884263871c932620b.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">难道是我的错觉吗？为什么感觉大家现在化妆很少画眼线了？朋友说她感觉画眼线压眼睛，显得眼睛更小了？但是画眼线不是可以让眼睛变得更大一些吗，怎么会显得眼睛更小呢，我有点get不到。。。</span></p>');
INSERT INTO `blog` VALUES (6, '这到底是做啥用的', 4, 2, '2025-08-06 15:42:58', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/54ed33bd816a473c8b95ae35f117c6e2.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">一直记得某一年买化妆品有个赠品是这样的东西(其实我也不知道是什么😬） 想找出来搅和粉底液用 找呀找呀就是找不到 最后在我初中的笔袋里找到了这一个 感谢我初中的时候总买点乱七八糟的东西 当时应该是准备拿来当镜子上学带着去臭美的😂不过那时候买过好几个 现在全都找不着了 隐约记得这个特别容易划花 还有我初中笔袋的样子 到现在也觉得好看 果然我的审美十几年没有什么变化 感谢网络闺蜜们，帮我确定了这个就是小镜子也确定了这一个是买化妆品送的，我自己买的那些全都找不到了我这个撕了膜了，就是这样模糊不清楚，所以我才一直不确定它到底是做什么用的</span></p>');
INSERT INTO `blog` VALUES (7, '一个疑惑：你们考研期间都化什么妆啊？', 4, 2, '2025-08-06 15:43:47', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/ff48a3e78d384ffbb9504c6e08b2c550.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我感觉我自己学习学傻了，每天泡在当地图书馆里不能自拔，对着镜子里的自己看呆呆的，脸上没有一点起色，想早上早起化个精致的妆容改善下自己，可还要早起抢图书馆的座位😭考研搭子一直劝我，姐妹们，谁能给我个妆容参考下，感激不尽~</span></p>');
INSERT INTO `blog` VALUES (8, '替领导开会签完到提前跑了', 7, 3, '2025-08-06 15:46:34', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/9328b1e595ae4b678fbefd0a8029013e.jpg', '', '图片', '<p><span style=\"font-size: 16px;\">替领导开会签完到提前跑了，结果没想到，人家还签退，领导被全县通报了 后续：今天领导见到我说，小萝啊，还要情况说明，我乖乖的写了情况说明给他审阅，这件事应该就这么过去了 没想到这个帖子火了，大家的祸都闯的五花八门，后续就是我给他写了情况说明，一会儿再给他送到县里去，他其实是有别的会冲突了，就派我去了个以为不太重要的视频会，他没想到我早跑，我也没想到会签退</span><span style=\"color: rgba(51, 51, 51, 0.6); font-size: 14px;\"><br></span></p>');
INSERT INTO `blog` VALUES (9, '叠纸游戏2026校园招聘正式启动！', 7, 3, '2025-08-06 15:47:21', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/56b4a089d7784094b4f4a5ddf72d81ef.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">🌟面向对象：2026届应届毕业生（毕业时间在2025.09-2026.08） 🌟网申时间：8月1日-10月31日 🌟岗位类别：程序技术类、美术表现类、产品策划类、市场运营类、IP开发类、质量管理类、游戏音频类、职能支持类</span></p>');
INSERT INTO `blog` VALUES (10, '求助 开车45分钟通勤的班要去吗？', 7, 3, '2025-08-06 15:49:52', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/ecf91ead89c94824adabbcbf9d145343.webp', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">如题，找到了个双休的工作，但是有点偏僻，坐车根本到不了，准备开车上班，8.30上班，通勤45分钟 我自己对于开车上班还有新鲜感，但是不知道会不会后面就很疲惫，求告知。</span></p><p><br></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">大家的回复我都有看到，上班通勤真的是大家共同的殇，坚持那么久的都是超级厉害的人呀，太长通勤的也要照顾好自己哦，不要太辛苦了。 回到我本身，这份工作出现的时间太巧了，双休，工资也合适，听招聘方说工作量也不大，冬天还会延迟半小时上班。 对开车上班的新鲜感还在，以前在外地试过地铁公交单车，让我试试看开车上班吧！</span></p>');
INSERT INTO `blog` VALUES (11, '追星女就不可能辞职', 7, 3, '2025-08-06 15:51:39', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/c3e0685e99ca42bf9e803d0f908c5ea8.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/85322099df9443f88a6e0cb5c407453d.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">附了原图，欢迎大家交作业 ps 上班时间完成，用的是公司的打印机，公司的剪刀，公司的创可贴卷一卷贴上的</span></p>');
INSERT INTO `blog` VALUES (12, '好像不小心和侠缘说漏嘴了', 6, 3, '2025-08-06 15:53:24', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/47f33988566548ef9c2860f83b87d1f0.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">晚上和侠缘一起觉的时候 我们再次聊到了结缘的话题 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我依旧坚决的表示：我以后不进醉花阴</span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\"> 我侠缘则仍然回复那句反驳：那你以后碰到更喜欢的人了怎么办？ </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我：我只喜欢你 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我侠缘：你要是突然喜欢上别人了呢？ </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我：那就死 一系列重复的话术拉扯之后 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我脑海忽然闪过一句话 我：你是不是喜欢上别人了？ </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我侠缘一直没接话 此时已经凌晨一点多 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我不知道她是睡着了 还是沉默了 我不知道…</span></p>');
INSERT INTO `blog` VALUES (13, '从开口就冷场到句句点头，表达力让我翻盘', 8, 2, '2025-08-06 15:56:50', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/bba481867b4b4edebd2bcbdf27917388.png', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">心中有模板，脑中有框架，只要发言，张口就来 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">每天10分钟练口才，63天之后你会惊艳会所有人 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">巴菲特说:要学会演讲。这是一项可以持续使用五六十年的资产，如果你不喜欢演讲，就要承受五六十年的损失。 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">卡耐基说:一个人的成功85%靠他的人际沟通和演说能力，只有15%靠他的专业技能 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">马云说:如果你不会说话，你的人生就是十分耕耘一分收获。如果你学会说话，就是一分耕耘十分收获。 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">平时讲话自然流畅，一到台上就慌了神，一紧张就掉链子，怎么办？ </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">来学表达，学会了如同安上了一双翅搒，任何地方不怯场，自信、又把自己的观点通过表达种在别人心中，生根发芽。 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">学会当众讲话，提升表达影响力! </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">只要你敢于发出声音，就会有观众和粉丝，有追随者!加入表达创富营改变从此刻发生...</span></p>');
INSERT INTO `blog` VALUES (14, 'crush深夜在朋友圈发美照吸引男人如何调理', 8, 2, '2025-08-06 15:57:59', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/2551ee0f81c047c0aa99a9f193b2de6c.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">本人直女，在遇到这样的事情之前也没想到过人生竟然能如此戏剧化。 前期提要，本人小时候长时间患有一种影响外貌的疾病因此很一直很内向，也有点自卑，没怎么接触过异性。直到长大一些彻底摆脱了这种病，从此以后性情大变，成为了一种神奇的厚脸皮体质。有次在某个旅游景区打黑车的时候，一起拼车的帅哥真的是一眼踩在了小女子审美点上，白白净净的小脸加一副斯斯文文的眼镜，抓得恰到好处的微分碎盖配合眼角的泪痣，1.85(目测）的完美身高更是馋得小女子直呼快哉快哉，于是果断要到了帅哥微叉，旅游结束以后我们开始在网上热聊。 聊天以后发现他并非徒有其表，他很会聊天，风趣幽默，每次跟他聊天的愉快程度让我不禁怀疑这是不是老天专门针对我下的杀猪盘，但是在此等尤物的强烈吸引力面前，相信没有谁能抵制住这种诱惑，于是一场针对我的巨大服从性测试由此拉开了帷幕。 鄙人平时很喜欢视奸他的朋友圈，因为这位小帅哥深谙朋友圈美学，审美不错，拍出来的照片赏心悦目，多看两眼咱大女人才有力气讨生活，并且还十分情感丰富，生活中遇到的一些琐事也能被他扩写成一篇文笔细腻的小作文。由此引出了关键的部分，在一个夜深人静的夜晚，小女子朋友圈刷新出来一条十分吸引眼球的他的九宫格美照，看得人目眦欲裂，目瞪口呆，每一张都是极其美艳，直男根本无法想象的拍照姿势和角度，其娇媚程度令人叹服，配合着一段钓系情感文案：这个世界欠调教，我也是。此时再点进他的朋友圈(可能是手滑不小心给我分组分错了)，里面记录了他为各任前男友写的小作文，比如其中有一任是他的金主爸爸，帅气多金的老男人，但对他只有生理需求毫无情感；还有一任是顶帅情绪价值拉满的男大，但是玩的花在全国各地范围内均有男女老少形形色色的对象。 看完以后我五感尽失，就这么茫茫然地沉沉睡去了，第二天早上起来的时候发现他所有朋友圈我都看不到了。在下觉得他应该是发现给我分组分错了，于是单删了我，但是至今不敢去给他发消息验证。由此达成了隐藏结局——湾仔码头。</span></p>');
INSERT INTO `blog` VALUES (15, '杭州花店门面景观改造', 9, 1, '2025-08-06 15:59:13', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/0021f224433142f2b5b52f6e33b9f309.jpg', '', '图片', '<p>改造中</p>');
INSERT INTO `blog` VALUES (16, '4.30号现版本唯一完美阵容，学会轻松王者！', 6, 4, '2025-08-06 16:00:01', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/fb55cf64a62d461b84561925e70d087a.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">🔥阵容优点：现版本唯一完美阵容，过渡简单平滑，中期强势提速 🔥阵容分析：现版本卡莎在独家的情况下绝对可以说是最强阵容，中期强势提速，后期三星后可以打一切阵容，是当之无愧的版本之子。通常阵容都有缺点，而卡莎作为当前版本的完美阵容，唯一的缺点就是，怕同行，三星不了。 基础构架： 【二星守】芮尔 卡莎 【三未来】潘森 卢锡安 肾 搭配挂件（以下方案选择其一） 【搭挂件】费德提克 厄运小姐 莫甘娜 剑魔 【搭星守】艾克+辛德拉（推荐辛德拉来得早或有星守转） 【搭护卫】盖伦+瑞雯（推荐外面物理较多或有护卫转时） ❗在七人口的时候组成基础构架再搭配两张，有星守转给女枪，有护卫转给潘森 ❗七人口整体二星后看数量和同行情况选择追三星卡莎芮尔，还是选择上八提质量 🔥运营技巧： 开局抢攻速，优先做出核心装电刃，其次是鬼索狂暴之刃 【2阶段】什么二星上什么，优选三未来，护卫迅捷过渡 【3阶段】3-2升6级，小搜组成基础构架+1挂件，如质量较高，则继续过渡到4阶段 【4阶段】4-1升7搜出大部分二星，根据转职情况，选择基础构架搭配方案，无转职选则搭两挂件 【5阶段】根据卡莎数量决定追3星或升8补费德提克等强力挂件 【6阶段】追出三星卡莎再升9，通常情况下三星卡莎足以吃鸡 🔥装备方面： 卡莎： 必备（启动装二选一）：鬼索狂暴之刃/朔极之矛 备选：电刃，珠光护手，科技枪，帽子 芮尔：龙牙，反甲，板甲，狂徒 副C厄运小姐：朔极之矛，珠光护手，巨人杀手，帽子等法系输出装 满足上诉装备后，多余装备分配如下： 稻草人：离子火花，珠光护手，鬼书 优选英雄强化（强度按先后顺序）： 芮尔的坚守防线 卡莎的多重射击 其他体系内强化 最强强化：好事成双（两个挂件选择再上一个芮尔和卡莎） 优选强化： 星守转职，珠光莲花，护卫转职，迅捷射手之心 其他尽量选战力海克斯，如：狩猎律动，源计划甲壳 经济海克斯只选择dd街区 黄金门票 写在最后：整个s8赛季，其实卡莎是最强的阵容，没有之一，贯穿整个s8-s8.5，比贾克斯还要毒瘤的存在，但由于金铲铲卡莎一直有bug，所以卡莎一直是最弱的阵容。在卡莎修复bug后，终于在金铲铲迎来了曙光，乘着玩的人不多，快冲！</span></p>');
INSERT INTO `blog` VALUES (17, '期中考学生考太差不用上课了', 1, 4, '2025-08-06 16:01:25', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/7825895032f14000985be2a0bd79937b.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">在机构兼职教初三物理 学生初二下学期期末考30多分 上课前老板跟我说他现在觉悟了，想好好学了 教了一个月（一星期一节课）后第一次月考48分，年级上进步了四五十名 但是分析试卷还是有好多重复讲过的题目做错了，把这些讲过做错了的分加上去可以考60，就定了60分为期中考目标 期中考前最后一次课他说这次考到欧姆定律前的知识，计算题考两道热学的。最后一节课就着重复习了热学部分（前几节课讲的电学） 最后期中考出来得了36分😢</span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">考完上课分析试卷他还沾沾自喜地说这个分数还进步了7名，把我都给气笑了 最后大题一道电学一道热学，基本没做出来 教的我也很心累，上课看似在认真的听，实则根本没有在动脑，重复讲了n多遍一会儿会一会儿不会。有时数学老师在隔壁给他上课，能听到数学老师被气得梆梆梆敲白板 本来是明早的课，突然说取消了，换老师了，虽然有点意外，但还是反思了一下自己教学中的不足 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">1.不成体系 物理教学时间不长，除了初三知识点，初二的遗忘较多。没有一个完整的上课体系，每次上课前都是先问学生想上哪部分，比较被动，知识东上一点西上一点，连贯性不足 2.不够严厉 基本没对学生发过火说过重话，有时学生状态不好（心不在焉）也只是温柔提醒一下，调整不过来也就不管顺着讲了 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">3.容易受学生情绪影响 另一个物理学生，从第一次上课就耷拉着脸，一脸不爽，挺聪明的学生，但上到后面就会不耐烦，做笔记什么的鬼画符，看到她的脸就很影响心情，后来也主动提出来不给她上课了 </span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">综上，虽然有一定问题，但教学过程中自己也做到认真负责，学生学不会真的很挫败啊 没事了，到月底彻底告别教培，跳出舒适圈</span></p>');
INSERT INTO `blog` VALUES (18, '不磨脚缎带', 2, 4, '2025-08-06 16:02:23', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/7b74bc55c59441c78d97074b22701985.jpg', '', '图片', '<p>分享穿搭</p>');
INSERT INTO `blog` VALUES (19, '下一个风口：“东方玄学+中药、水晶手串”贼猛', 2, 4, '2025-08-06 16:03:20', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/c144c43180e84054a1ee92e2a576e8ef.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">在TikTok卖饰品，我靠“东方文化韵味”打了场翻身仗！爆了几千🥚！ 去年还在跟风卖流行款，卷到利润薄如纸，直到把普通白水手串包装成“月亮的眼泪·清透晶串”，配上东方chuan说故事和月光下的视频，三天卖光库存，才算摸到门道 中药手串+东方xuan学比水晶还猛！ 核心就是给饰品裹上“文化外衣”，这招让水晶、中药手串、银饰全爆单了！！ 具体怎么做给大家拆解一下 1️⃣水晶手串卖“自然意境”：改名“星辰守hu·白水串”，说它在东方文化里被视作自然纯粹的象征，能让人联想到澄澈的天地。视频拍模特在晨光中戴水晶做瑜伽，配风铃声，强调“天然矿物”“手工编织”，只提“营造宁静心境”，绝不说特殊作用 2️⃣中药手串卖“自然禅意”：檀香木串叫“沉境之息·冥xiang珠”，讲它在东方传统中常与静心时刻相伴，自带沉稳气息 视频拍熏香与旧书，模特戴串深呼吸，配颂钵音，突出“天然原木”“独特木纹”，只谈文化里的象征意义，不沾任何功效，但视频故事能让老外自己悟！ 3️⃣银饰卖“文化符号”：平安扣项链变“如意结·东方银饰”，说绳结是古老东方的吉祥符号，代表美好联结 视频拍老人编中国结、女孩戴链自信行走，强调“手工工艺”“传统符号现代演绎”，用“美好寓意”替代特殊说法 这套打法能成，关键在四点： 1️⃣老外对东方文化充满好奇，觉得有故事的东西够独特； 2️⃣卖的是情绪价值和文化体验，让饰品成为承载情感的物件 3️⃣天然、手工、独特性正对他们胃口 4️⃣精准打</span><a href=\"https://www.xiaohongshu.com/search_result?keyword=spirituality&amp;type=54&amp;source=web_note_detail_r10\" target=\"_blank\" style=\"text-align: start;\">#spirituality</a><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\"> </span><a href=\"https://www.xiaohongshu.com/search_result?keyword=easternwisdom%25E7%25AD%2589%25E6%25A0%2587%25E7%25AD%25BE%25E5%25BC%2595%25E6%25B5%2581&amp;type=54&amp;source=web_note_detail_r10\" target=\"_blank\" style=\"text-align: start;\">#easternwisdom等标签引流</a><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">，且严守红线不谈任何功效类表述 现在看啥饰品都能加东方元素：素圈戒指讲“圆满”的寓意，珍珠叫“月魄凝华”的雅称。用老外能理解的“东方文化”语言包装这些元素，在饰品赛道就是降维打击！！</span></p>');
INSERT INTO `blog` VALUES (20, '我好爱这个反派！', 5, 5, '2025-08-06 16:04:51', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/d88a5910ce1149d38879f477577927aa.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我好爱模范出租车2里的这个反派角色，我觉得他长的好帅啊！</span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">因为被人误导做了坏事，最后我觉得不算洗白 如果他先遇到的是主角团，一定会变得很好吧</span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">就连最后流泪的时候都是在回忆出租车行的日子，他其实非常需要真诚的同伴和关爱啊</span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">第三季能不能把他复活，让他多做好事赎罪啊啊啊</span></p>');
INSERT INTO `blog` VALUES (21, '底特律一周目结局记录', 5, 5, '2025-08-06 16:05:40', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/6b4553a90010436faf83dd9a160f092e.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/8593646a020b4b019a65eb81f27ae8cb.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">马库斯领导革命胜利 Alex和卡拉存活 但是康纳中途就反库了</span></p><p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">感觉他那条线最精彩但是没打到</span></p>');
INSERT INTO `blog` VALUES (23, '汤姆猫的梦中情猫', 10, 5, '2025-08-06 16:08:26', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/4cf1e9795dd046c197e37f981dc38526.png', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255);\">一戴上蝴蝶结就鬼迷日眼的</span></p>');
INSERT INTO `blog` VALUES (25, '地铁工作人员 合肥', 7, 1, '2025-08-10 13:29:13', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/523606e1f8ea409c9d0944084f288f6b.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/ab6bcf2f771046a9a71a0e1a78c984a9.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">地铁工作人员有编制吗，坐标合肥，薪资怎么样</span></p>');
INSERT INTO `blog` VALUES (26, '差点把我看呕了，旅顺博物馆这两古代尸体', 5, 1, '2025-08-10 13:30:27', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/55f3e9bb86ae45368c7e1cc7ea4dbe23.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/94e65fe2f80a47dbbe2db89b4d6c2172.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">真的有点生理不适，皮肤都变成鱿鱼干了。</span></p>');
INSERT INTO `blog` VALUES (27, '抱歉，我家真的不接待剧组上门拍戏😅', 8, 1, '2025-08-10 13:31:25', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/eebd24bf76e149f79e67d55fc8897c80.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/eddeb897ad3243b3aff63d6195b8dbd6.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">我的农村自建房陆续收到了不少短剧剧组的邀约 剧组联系说房子有点老洋房的味道， 想要租用几天拍摄… 🖐🏻位数的场地费虽然很可观！ 但仔细考虑了一下还是拒绝了！ 毕竟房子是自己住， 运输摄影器材难免会有磕碰 i人还是自己安安静静宅着才舒服～</span></p>');
INSERT INTO `blog` VALUES (28, '南京博物院你真的很人性化', 8, 1, '2025-08-10 13:32:54', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/8ef22a50a1754bfa9649a81d43b111e4.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">说实话，我逛了那么多博物馆，第一次在一个博物馆里看到了高峰期时，女生的专用卫生间，成功在3分钟之内上到了厕所，南博你好人性化</span></p>');
INSERT INTO `blog` VALUES (29, '武汉东湖磨山好美', 8, 1, '2025-08-10 13:33:45', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/3fe79b5677fb4effb701bd0033c1b743.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/bcd04decc87d4174956ce9cf482b5a58.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255);\">武汉东湖磨山好美</span></p>');
INSERT INTO `blog` VALUES (30, '千万别入坑', 6, 1, '2025-08-10 13:38:54', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/d2df9cd8d09644208e3ff7b8728bbf61.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">新买的华为平板，游戏党慎重入手，系统太封闭了。</span></p>');
INSERT INTO `blog` VALUES (31, '哇！台风来临前的家', 9, 1, '2025-08-10 13:40:38', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/eaa28fce7f5f42b5a9e75c4b22bc46cc.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/f4ee8a5011204ebeaecd848ce8c5230e.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">傍晚到家7点多 推开家门后真的被美到 台风前的天空 真的是七彩的 匆忙的拍了几张live 分享调色盘给大家🎨</span></p>');
INSERT INTO `blog` VALUES (32, '小猫的自我介绍', 10, 5, '2025-08-06 16:10:32', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/cd0230509ceb456abf34cbaabd321770.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/d57db38ada744f6385e270646bc6754b.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255);\">这是来自小猫的自我介绍</span></p><p><br></p>');
INSERT INTO `blog` VALUES (33, '是暑假带娃观影的最佳选择呀', 5, 5, '2025-08-06 16:06:30', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/251d8e04ebbf42bab0192ba650551499.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/fd1e8a18bc154b43b628d332c4c9e60d.mp4', '视频', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">大人儿童都能笑成一片， 只不过大人笑着笑着就沉默了， 因为真的很容易带入自身的现状， 那是因为真的很写实 （努力了好久还没上岸的样子， 面对最亲的人只能撒谎； 工作遇到困难， 面对父母只能报喜不报忧。） 从故事情节来说： 从我们熟知的故事出发， 除了有在西游世界的取经四人组， 还有另一个取经四人组。 让我们看到了西游故事的另一半。 从画面色调来说： 是真的对眼睛好啊， 草地、山间、乡村、水流等等 绿色调搭配真的好夏天 配色很美。 还有就是有的人物虽然只有寥寥几笔， 但是却能活灵活现体现出人物的灵魂。 看到结尾，突然想到取经路上的种种， 我们小时候看懂了吗？ 是我们知道的那样吗？ 大人物为了取得想要的效果， 是会为底层的草根的生存考虑的吗？ 最后引用片尾的一句话： “致我们都能活成自己想要的样子” 还有就是片中突然来一段陕北口音真的还蛮搞笑的哈哈哈哈哈哈哈哈哈😂</span></p>');
INSERT INTO `blog` VALUES (34, '一大早被巴黎警察这个纹身笑死了', 7, 1, '2025-08-10 13:35:05', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/d351302e477b4111ba13e977d1a729ad.jpg', '', '图片', '<p><span style=\"color: rgb(51, 51, 51); background-color: rgb(255, 255, 255); font-size: 16px;\">纹身师傅也二， 顺序应该是 水木金火土 吧</span></p>');
INSERT INTO `blog` VALUES (37, '去年夏天', 3, 1, '2025-11-11 22:52:45', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/c366c9a078cb41e1ade9c0ee2ca2ae40.png', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/7092702464f94476a40b404e801ec231.mp4', '视频', '<p>123</p>');
INSERT INTO `blog` VALUES (38, '美食', 3, 1, '2025-11-12 11:22:59', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/a846a8e533334961a24a56125a26f13e.jpg', '', '图片', '<p>123</p>');

-- ----------------------------
-- Table structure for chat
-- ----------------------------
DROP TABLE IF EXISTS `chat`;
CREATE TABLE `chat`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文本内容',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文本类型',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '创建时间',
  `from_user_id` int NULL DEFAULT NULL COMMENT '来自用户id',
  `to_user_id` int NULL DEFAULT NULL COMMENT '发往用户id',
  `is_read` tinyint NULL DEFAULT NULL COMMENT '是否已读',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 80 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '聊天管理' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of chat
-- ----------------------------
INSERT INTO `chat` VALUES (18, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/dac2360535f04b6e9b9a781c66e3aa07.jpg', '图片', '2025/11/29 15:55:36', 1, 5, 1);
INSERT INTO `chat` VALUES (19, '你好', '文字', '2025/11/29 15:55:53', 1, 5, 1);
INSERT INTO `chat` VALUES (20, '你好', '文字', '2025/12/1 12:02:10', 5, 1, 1);
INSERT INTO `chat` VALUES (21, '你好\n', '文字', '2025/12/1 12:02:32', 5, 1, 1);
INSERT INTO `chat` VALUES (22, '123', '文字', '2025/12/18 20:44:12', 1, 5, 1);
INSERT INTO `chat` VALUES (23, '123', '文字', '2025/12/18 20:46:59', 1, 5, 1);
INSERT INTO `chat` VALUES (24, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/69f58134435c4f75827421364e7753f0.txt', '文件', '2025/12/18 20:54:18', 1, 5, 1);
INSERT INTO `chat` VALUES (25, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/c02b23fa234542c898e1eb364a95156c.txt', '文件', '2025/12/18 20:56:56', 1, 5, 1);
INSERT INTO `chat` VALUES (26, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/89fc835c875842f6a7b55f0c08b7dde8.txt', '文件', '2025/12/18 21:01:13', 1, 5, 1);
INSERT INTO `chat` VALUES (27, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/8ebabe92377f418cac98578965b495b1_AI%E7%94%9F%E6%88%90%E7%8E%B0%E4%BB%A3%E8%AE%BA%E5%9D%9B%E9%A1%B5%E9%9D%A2%E7%9A%84%E8%AF%A6%E7%BB%86%E6%8F%90%E7%A4%BA%E8%AF%8D%E6%A8%A1%E6%9D%BF.txt', '文件', '2025/12/18 21:14:09', 1, 5, 1);
INSERT INTO `chat` VALUES (28, '通话时间：00:07', '通话记录', '2025/12/18 23:03:09', 5, 1, 1);
INSERT INTO `chat` VALUES (29, '通话时间：00:07', '通话记录', '2025/12/18 23:03:09', 1, 5, 1);
INSERT INTO `chat` VALUES (30, '😀', '文字', '2025/12/19 11:00:41', 1, 5, 1);
INSERT INTO `chat` VALUES (31, '语音通话时间：00:02', '通话记录', '2025/12/19 11:16:16', 1, 5, 1);
INSERT INTO `chat` VALUES (32, '语音通话时间：00:02', '通话记录', '2025/12/19 11:16:16', 5, 1, 1);
INSERT INTO `chat` VALUES (33, '视频通话时间：00:18', '视频通话记录', '2025/12/20 21:10:14', 1, 5, 1);
INSERT INTO `chat` VALUES (34, '视频通话时间：00:18', '视频通话记录', '2025/12/20 21:10:14', 5, 1, 1);
INSERT INTO `chat` VALUES (35, '视频通话时间：00:29', '视频通话记录', '2025/12/20 21:14:14', 1, 5, 1);
INSERT INTO `chat` VALUES (36, '视频通话时间：00:29', '视频通话记录', '2025/12/20 21:14:14', 5, 1, 1);
INSERT INTO `chat` VALUES (37, '视频通话时间：00:11', '视频通话记录', '2025/12/20 21:15:39', 1, 5, 1);
INSERT INTO `chat` VALUES (38, '视频通话时间：00:11', '视频通话记录', '2025/12/20 21:15:39', 5, 1, 1);
INSERT INTO `chat` VALUES (39, '视频通话时间：00:21', '视频通话记录', '2025/12/20 21:23:58', 5, 1, 1);
INSERT INTO `chat` VALUES (40, '视频通话时间：00:21', '视频通话记录', '2025/12/20 21:23:58', 1, 5, 1);
INSERT INTO `chat` VALUES (41, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/3ed5341a5d57481dae368eb4bc8858ad_1+%2812%29.jpg', '图片', '2025/12/20 21:42:50', 5, 1, 1);
INSERT INTO `chat` VALUES (42, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/7dc713c4ccf4489fae22ba4128cb74c4_1+%2813%29.jpg', '图片', '2025/12/20 21:42:55', 5, 1, 1);
INSERT INTO `chat` VALUES (43, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/ebfd5e3535c44e5cac44e7feb0bbea61_1+%2840%29.jpg', '图片', '2025/12/20 21:42:59', 5, 1, 1);
INSERT INTO `chat` VALUES (44, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/7d12bda3668c42ff82385dcbf77c70b6_5+%288%29.jpg', '图片', '2025/12/20 21:43:03', 5, 1, 1);
INSERT INTO `chat` VALUES (45, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/644faf5581c74f7b84de1cd7141e4266_1+%2812%29.jpg', '图片', '2025/12/20 21:44:25', 5, 1, 1);
INSERT INTO `chat` VALUES (46, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/284bda094f73438eb408d478dde9c19e_1+%2840%29.jpg', '图片', '2025/12/20 21:44:37', 5, 1, 1);
INSERT INTO `chat` VALUES (47, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/bc2baa521b944f58b2c1b9bca246e396_38b6b90c83c5475e967b740afc007860%7Etplv-a9rns2rl98-pc_smart_face_crop-v1_480_360.jpg', '图片', '2025/12/20 21:45:00', 5, 1, 1);
INSERT INTO `chat` VALUES (48, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/29a5971b67b540fe96fbd8c4f0a38884_AI%E7%94%9F%E6%88%90%E7%8E%B0%E4%BB%A3%E8%AE%BA%E5%9D%9B%E9%A1%B5%E9%9D%A2%E7%9A%84%E8%AF%A6%E7%BB%86%E6%8F%90%E7%A4%BA%E8%AF%8D%E6%A8%A1%E6%9D%BF.txt', '文件', '2025/12/20 21:45:10', 5, 1, 1);
INSERT INTO `chat` VALUES (49, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/0421109ac6b94164bf5a00dea7dfda9c_38b6b90c83c5475e967b740afc007860%7Etplv-a9rns2rl98-pc_smart_face_crop-v1_480_360.jpg', '图片', '2025/12/20 21:50:51', 5, 1, 1);
INSERT INTO `chat` VALUES (50, 'http://127.0.0.1:9090/web/download/547f0251a697474b8eecd6d8d73891a1.jpg', '图片', '2025/12/20 22:12:48', 5, 1, 1);
INSERT INTO `chat` VALUES (51, 'http://127.0.0.1:9090/web/download/2498c8afb88a4a8c9e54c51a318a1b96.jpg', '图片', '2025/12/20 22:18:10', 5, 1, 1);
INSERT INTO `chat` VALUES (52, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/1ef026c869ab4c94b68ea146289f6e5e_38b6b90c83c5475e967b740afc007860%7Etplv-a9rns2rl98-pc_smart_face_crop-v1_480_360.jpg', '图片', '2025/12/20 22:18:56', 5, 1, 1);
INSERT INTO `chat` VALUES (53, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/1c7e5578bbd1475b88d8a64cd9441c78_38b6b90c83c5475e967b740afc007860%7Etplv-a9rns2rl98-pc_smart_face_crop-v1_480_360.jpg', '图片', '2025/12/20 22:26:11', 5, 1, 1);
INSERT INTO `chat` VALUES (54, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/22da6d702bfd491a9c8e5b27c014c72a_4bcfd335e8672bf81616ea7893b502df%7E480x480.jpg', '图片', '2025/12/20 22:27:12', 5, 1, 1);
INSERT INTO `chat` VALUES (55, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/7982f7e8741249fc8ecd284b523f732e_4bcfd335e8672bf81616ea7893b502df%257E480x480.jpg', '图片', '2025/12/20 22:32:21', 5, 1, 1);
INSERT INTO `chat` VALUES (56, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/60423117280e442f8ef1c856515cfed8_AccessKey.csv', '文件', '2025/12/20 22:51:09', 5, 1, 1);
INSERT INTO `chat` VALUES (57, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/c4afe8a24fbf48679e912f2c4af7d7dd_4337e81bd5784c21984de88f01c5e30e.gif', '图片', '2025/12/20 22:55:31', 5, 1, 1);
INSERT INTO `chat` VALUES (58, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/335e17b7c13745749a48cd89911e5db5_4bcfd335e8672bf81616ea7893b502df%7E480x480.jpg', '图片', '2025/12/20 22:58:33', 5, 1, 1);
INSERT INTO `chat` VALUES (59, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/afe999100e234cfca091a68df52b376e_4337e81bd5784c21984de88f01c5e30e.gif', '图片', '2025/12/20 22:58:58', 5, 1, 1);
INSERT INTO `chat` VALUES (60, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/43526f5f93e74c0abb8b622c73c21885_38b6b90c83c5475e967b740afc007860%7Etplv-a9rns2rl98-pc_smart_face_crop-v1_480_360.jpg', '图片', '2025/12/20 22:59:04', 5, 1, 1);
INSERT INTO `chat` VALUES (61, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/c52ecc412588435c85daff1ca51cb2e5_01be655df09599a801219cce9660c7.jpg%401280w_1l_2o_100sh.jpg', '图片', '2025/12/20 22:59:10', 5, 1, 1);
INSERT INTO `chat` VALUES (62, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/5857db4526294e79b748edebe973a50a_4bcfd335e8672bf81616ea7893b502df%7E480x480.jpg', '图片', '2025/12/20 23:08:04', 5, 1, 1);
INSERT INTO `chat` VALUES (63, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/de913f1d930b4bb5a0770fb397796a37_38b6b90c83c5475e967b740afc007860%7Etplv-a9rns2rl98-pc_smart_face_crop-v1_480_360.jpg', '图片', '2025/12/20 23:08:15', 5, 1, 1);
INSERT INTO `chat` VALUES (64, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/ed570dd30dba48c38a2819432297889e_559d7048277d4db6946a289a8f5f562c.png', '图片', '2025/12/20 23:10:40', 5, 1, 1);
INSERT INTO `chat` VALUES (65, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/0865c4ee7373411fbdd45f2ed4af0907_4337e81bd5784c21984de88f01c5e30e.gif', '文件', '2025/12/20 23:10:47', 5, 1, 1);
INSERT INTO `chat` VALUES (66, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/34d2370258fd46dc846285afc47d48f3_38b6b90c83c5475e967b740afc007860%7Etplv-a9rns2rl98-pc_smart_face_crop-v1_480_360.jpg', '文件', '2025/12/20 23:11:25', 5, 1, 1);
INSERT INTO `chat` VALUES (67, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/6ceed466f2b2419a8361140d9e6c535d_4bcfd335e8672bf81616ea7893b502df%7E480x480.jpg', '图片', '2025/12/20 23:12:07', 5, 1, 1);
INSERT INTO `chat` VALUES (68, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/b64f77ead8b54fffbd8f52a243c72b0a_51380a1bd8c7d3ddabc725b9af989780%7E480x480.jpg', '图片', '2025/12/20 23:12:18', 5, 1, 1);
INSERT INTO `chat` VALUES (69, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/01c65a6a0d9d4abd83ba1be2d201c3ec_OIP-C+%284%29.webp', '图片', '2025/12/20 23:12:32', 5, 1, 1);
INSERT INTO `chat` VALUES (70, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/8535bacdfde44efdaab8efb35847958d.jpg', '图片', '2025/12/21 11:27:16', 5, 1, 1);
INSERT INTO `chat` VALUES (71, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/28d2ad36cf3041a28ba6bfecdb1f77eb.jpg', '文件', '2025/12/21 11:27:31', 5, 1, 1);
INSERT INTO `chat` VALUES (72, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/c5463206014541aea6297f29768e55d0.jpg', '图片', '2025/12/21 11:27:47', 5, 1, 1);
INSERT INTO `chat` VALUES (73, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/f6126acccc2f417ab3e4f3e20403fc27.txt', '文件', '2025/12/21 11:28:27', 5, 1, 1);
INSERT INTO `chat` VALUES (74, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/7798c299026347bdac41588b7895e373.jpg', '图片', '2025/12/21 11:33:24', 5, 1, 1);
INSERT INTO `chat` VALUES (75, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/4e6c10435002459296abc8372e7c77ad.txt', '文件', '2025/12/21 11:33:34', 5, 1, 1);
INSERT INTO `chat` VALUES (76, '{\"code\":\"401\",\"msg\":\"无token，请重新登录\",\"data\":null}', '文件', '2025/12/21 11:38:19', 5, 1, 1);
INSERT INTO `chat` VALUES (77, '{\"code\":\"401\",\"msg\":\"无token，请重新登录\",\"data\":null}', '文件', '2025/12/21 11:38:34', 5, 1, 1);
INSERT INTO `chat` VALUES (78, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/9c2fda92bba54e479f9a249fd2b95eda_AI%E7%94%9F%E6%88%90%E7%8E%B0%E4%BB%A3%E8%AE%BA%E5%9D%9B%E9%A1%B5%E9%9D%A2%E7%9A%84%E8%AF%A6%E7%BB%86%E6%8F%90%E7%A4%BA%E8%AF%8D%E6%A8%A1%E6%9D%BF.txt', '文件', '2025/12/21 11:57:08', 5, 1, 1);
INSERT INTO `chat` VALUES (79, 'https://iblog-file.oss-cn-beijing.aliyuncs.com/6b5020530adc477d9fc5c5009cf47c9e.jpg', '图片', '2025/12/21 11:57:34', 5, 1, 1);

-- ----------------------------
-- Table structure for collect
-- ----------------------------
DROP TABLE IF EXISTS `collect`;
CREATE TABLE `collect`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `item_id` int NULL DEFAULT NULL COMMENT '博客ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `item_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 97 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of collect
-- ----------------------------
INSERT INTO `collect` VALUES (76, 1, 33);
INSERT INTO `collect` VALUES (96, 1, 34);
INSERT INTO `collect` VALUES (35, 2, 27);
INSERT INTO `collect` VALUES (74, 2, 34);
INSERT INTO `collect` VALUES (75, 3, 34);

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '时间',
  `content` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '评论内容',
  `pid` int NULL DEFAULT NULL COMMENT '父评论ID',
  `origin_id` int NULL DEFAULT NULL COMMENT '最外层评论ID',
  `item_id` int NULL DEFAULT NULL COMMENT '博客ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of comment
-- ----------------------------
INSERT INTO `comment` VALUES (7, 1, '2025-11-07 19:18:44', '你好', NULL, NULL, 34);
INSERT INTO `comment` VALUES (8, 1, '2025-11-07 19:19:56', '111', NULL, NULL, 34);
INSERT INTO `comment` VALUES (9, 1, '2025-11-07 19:21:26', '确实挺不错的', NULL, NULL, 33);
INSERT INTO `comment` VALUES (10, 1, '2025-11-07 19:21:38', '666', NULL, 9, 33);
INSERT INTO `comment` VALUES (11, 1, '2025-11-07 19:21:47', '你在干啥', 10, 9, 33);
INSERT INTO `comment` VALUES (12, 2, '2025-11-07 19:22:16', '222', NULL, 8, 34);
INSERT INTO `comment` VALUES (13, 1, '2025-11-07 23:15:27', '测试一下', NULL, NULL, NULL);
INSERT INTO `comment` VALUES (14, 1, '2025-11-07 23:16:17', '测试一下', NULL, NULL, 34);
INSERT INTO `comment` VALUES (15, 1, '2025-11-07 23:16:25', 'okok', NULL, 14, 34);
INSERT INTO `comment` VALUES (16, 1, '2025-11-11 12:19:25', '你个**', 12, 8, 34);
INSERT INTO `comment` VALUES (17, 1, '2025-11-14 13:41:38', '1111', NULL, NULL, 32);
INSERT INTO `comment` VALUES (18, 1, '2025-11-14 13:41:48', '2222', NULL, 17, 32);
INSERT INTO `comment` VALUES (19, 5, '2025-12-21 17:23:04', '11', NULL, NULL, 38);

-- ----------------------------
-- Table structure for follow
-- ----------------------------
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `item_id` int NULL DEFAULT NULL COMMENT '博客ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `item_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 84 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of follow
-- ----------------------------
INSERT INTO `follow` VALUES (53, 1, NULL);
INSERT INTO `follow` VALUES (54, 1, NULL);
INSERT INTO `follow` VALUES (55, 1, NULL);
INSERT INTO `follow` VALUES (40, 1, 2);
INSERT INTO `follow` VALUES (47, 2, 1);
INSERT INTO `follow` VALUES (50, 5, 1);

-- ----------------------------
-- Table structure for like
-- ----------------------------
DROP TABLE IF EXISTS `like`;
CREATE TABLE `like`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` int NULL DEFAULT NULL COMMENT '用户ID',
  `item_id` int NULL DEFAULT NULL COMMENT '博客ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `user_id`(`user_id` ASC, `item_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of like
-- ----------------------------
INSERT INTO `like` VALUES (98, 1, 32);
INSERT INTO `like` VALUES (76, 1, 33);
INSERT INTO `like` VALUES (90, 1, 34);
INSERT INTO `like` VALUES (97, 1, 38);
INSERT INTO `like` VALUES (35, 2, 27);
INSERT INTO `like` VALUES (74, 2, 34);
INSERT INTO `like` VALUES (75, 3, 34);
INSERT INTO `like` VALUES (99, 5, 34);

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `text` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通知内容',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '通知类型',
  `time` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '时间',
  `from_user_id` int NULL DEFAULT NULL COMMENT '发送通知的用户ID',
  `to_user_id` int NULL DEFAULT NULL COMMENT '接收通知的用户ID',
  `item_id` int NULL DEFAULT NULL COMMENT '关联的博客ID',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 46 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of message
-- ----------------------------
INSERT INTO `message` VALUES (1, '收藏了你的笔记！', '收藏', '2025-11-10 12:45:00', 2, 1, 34);
INSERT INTO `message` VALUES (2, '收藏了你的笔记！', '收藏', '2025-11-10 12:45:59', 3, 1, 34);
INSERT INTO `message` VALUES (3, '关注了你！', '关注', '2025-11-12 18:43:09', 1, 5, NULL);
INSERT INTO `message` VALUES (4, '收藏了你的笔记！', '收藏', '2025-11-12 18:44:43', 1, 5, 33);
INSERT INTO `message` VALUES (5, '点赞了你的笔记！', '点赞', '2025-11-12 20:54:56', 1, 1, 39);
INSERT INTO `message` VALUES (6, '点赞了你的笔记！', '点赞', '2025-11-12 20:54:59', 1, 1, 39);
INSERT INTO `message` VALUES (7, '点赞了你的笔记！', '点赞', '2025-11-12 20:55:03', 1, 1, 39);
INSERT INTO `message` VALUES (8, '点赞了你的笔记！', '点赞', '2025-11-12 20:56:33', 1, 1, 39);
INSERT INTO `message` VALUES (9, '点赞了你的笔记！', '点赞', '2025-11-12 21:00:05', 1, 1, 39);
INSERT INTO `message` VALUES (10, '收藏了你的笔记！', '收藏', '2025-11-12 21:00:08', 1, 1, 39);
INSERT INTO `message` VALUES (11, '收藏了你的笔记！', '收藏', '2025-11-12 21:00:10', 1, 1, 39);
INSERT INTO `message` VALUES (12, '点赞了你的笔记！', '点赞', '2025-11-12 21:00:37', 1, 1, 34);
INSERT INTO `message` VALUES (13, '点赞了你的笔记！', '点赞', '2025-11-12 21:00:42', 1, 1, 34);
INSERT INTO `message` VALUES (14, '点赞了你的笔记！', '点赞', '2025-11-12 21:03:21', 1, 1, 39);
INSERT INTO `message` VALUES (15, '收藏了你的笔记！', '收藏', '2025-11-12 21:03:23', 1, 1, 39);
INSERT INTO `message` VALUES (16, '收藏了你的笔记！', '收藏', '2025-11-12 21:05:56', 1, 1, 39);
INSERT INTO `message` VALUES (17, '收藏了你的笔记！', '收藏', '2025-11-12 21:06:02', 1, 1, 39);
INSERT INTO `message` VALUES (18, '点赞了你的笔记！', '点赞', '2025-11-12 21:16:04', 1, 1, 39);
INSERT INTO `message` VALUES (19, '收藏了你的笔记！', '收藏', '2025-11-12 21:16:06', 1, 1, 39);
INSERT INTO `message` VALUES (20, '点赞了你的笔记！', '点赞', '2025-11-12 21:16:19', 1, 1, 38);
INSERT INTO `message` VALUES (21, '收藏了你的笔记！', '收藏', '2025-11-12 21:16:21', 1, 1, 38);
INSERT INTO `message` VALUES (22, '收藏了你的笔记！', '收藏', '2025-11-12 21:18:52', 1, 1, 39);
INSERT INTO `message` VALUES (23, '收藏了你的笔记！', '收藏', '2025-11-12 21:18:53', 1, 1, 39);
INSERT INTO `message` VALUES (24, '收藏了你的笔记！', '收藏', '2025-11-12 21:29:57', 1, 1, 34);
INSERT INTO `message` VALUES (25, '点赞了你的笔记！', '点赞', '2025-11-12 22:40:21', 1, 5, 32);
INSERT INTO `message` VALUES (26, '点赞了你的笔记！', '点赞', '2025-11-12 23:04:12', 5, 1, 34);
INSERT INTO `message` VALUES (27, '关注了你！', '关注', '2025-11-13 11:46:01', 5, 1, NULL);
INSERT INTO `message` VALUES (28, '关注了你！', '关注', '2025-11-14 13:41:42', 1, 5, NULL);
INSERT INTO `message` VALUES (29, '关注了你！', '关注', '2025-11-16 18:58:07', 1, NULL, NULL);
INSERT INTO `message` VALUES (30, '关注了你！', '关注', '2025-11-16 18:58:10', 1, NULL, NULL);
INSERT INTO `message` VALUES (31, '关注了你！', '关注', '2025-11-16 18:58:11', 1, NULL, NULL);
INSERT INTO `message` VALUES (32, '关注了你！', '关注', '2025-11-16 18:59:18', 1, 5, NULL);
INSERT INTO `message` VALUES (33, '关注了你！', '关注', '2025-11-16 18:59:22', 1, 5, NULL);
INSERT INTO `message` VALUES (34, '关注了你！', '关注', '2025-11-16 19:00:11', 1, 5, NULL);
INSERT INTO `message` VALUES (35, '关注了你！', '关注', '2025-11-16 19:00:46', 1, 5, NULL);
INSERT INTO `message` VALUES (36, '关注了你！', '关注', '2025-11-16 19:08:12', 1, 5, NULL);
INSERT INTO `message` VALUES (37, '关注了你！', '关注', '2025-11-16 19:08:14', 1, 5, NULL);
INSERT INTO `message` VALUES (38, '关注了你！', '关注', '2025-11-16 19:08:19', 1, 5, NULL);
INSERT INTO `message` VALUES (39, '关注了你！', '关注', '2025-11-16 19:11:16', 1, 5, NULL);
INSERT INTO `message` VALUES (40, '关注了你！', '关注', '2025-11-29 15:40:12', 1, 5, NULL);
INSERT INTO `message` VALUES (41, '关注了你！', '关注', '2025-11-29 15:40:42', 1, 5, NULL);
INSERT INTO `message` VALUES (42, '关注了你！', '关注', '2025-11-29 15:40:44', 1, 5, NULL);
INSERT INTO `message` VALUES (43, '关注了你！', '关注', '2025-11-29 15:42:52', 1, 5, NULL);
INSERT INTO `message` VALUES (44, '关注了你！', '关注', '2025-11-29 15:42:54', 1, 5, NULL);
INSERT INTO `message` VALUES (45, '点赞了你的笔记！', '点赞', '2025-12-20 22:47:13', 5, 1, 37);

-- ----------------------------
-- Table structure for sys_admin
-- ----------------------------
DROP TABLE IF EXISTS `sys_admin`;
CREATE TABLE `sys_admin`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '管理员管理' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_admin
-- ----------------------------
INSERT INTO `sys_admin` VALUES (1, 'admin', '21232f297a57a5a743894a0e4a801fc3', '管理员', 'http://127.0.0.1:9090/web/download/fb1ee38bac7b4d489017eca589fea52b.jpg', 'admin@email.com', '18888888888');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '密码',
  `nickname` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像',
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '电话',
  `info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '简介',
  `show_collect` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '0' COMMENT '是否展示个人收藏',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户管理' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, '111', '698d51a19d8a121ce581499d7b701668', '哈基聪', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/7ff728208b224360a61eb2bae0ce1346.jpg', 'user@email.com', '18888888888', '爱猫人士,最爱小猫啦', 'true');
INSERT INTO `sys_user` VALUES (2, '222', 'bcbe3365e6ac95ea2c0343a2395834dd', '宇宙之大', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/189b31b9e4804bf38a530e8ca20a5ee3.jpeg', 'user@email.com', '18888888888', '我在南极拍下了企鹅与冰川的剪影', NULL);
INSERT INTO `sys_user` VALUES (3, '333', '310dcbbf4cce62f762a2aaa148d556bd', '明智之举', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/b42d059f36884e87a7957c3f50b71bdc.webp', 'xxxxx@163.com', '178866998877', '想必你也看过了一些风景～', NULL);
INSERT INTO `sys_user` VALUES (4, '444', '550a141f12de6341fba65b0ad0433500', '科幻', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/357e91c4c5ce4a9a978987d2f32f8095.webp', 'kehuan@163.com', '1789990009988', '她的升空用了七秒半', NULL);
INSERT INTO `sys_user` VALUES (5, '555', '15de21c670ae7c3f6f3f1f37029303c9', '愿望', 'https://iblog-file.oss-cn-beijing.aliyuncs.com/files/fe6b61620fe4440bb6a3adc529bbab8c.webp', 'ywywyw@qq.com', '179888667766', '愿望成真吧', NULL);
INSERT INTO `sys_user` VALUES (6, '666', 'fae0b27c451c728867a567e8c1bb4e53', NULL, NULL, NULL, NULL, NULL, '0');
INSERT INTO `sys_user` VALUES (7, '1963', '827ccb0eea8a706c4c34a16891f84e7b', NULL, NULL, NULL, NULL, NULL, '0');

-- ----------------------------
-- Table structure for type
-- ----------------------------
DROP TABLE IF EXISTS `type`;
CREATE TABLE `type`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类名称',
  `info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类说明',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of type
-- ----------------------------
INSERT INTO `type` VALUES (1, '萌宠', '萌宠');
INSERT INTO `type` VALUES (2, '游戏', '游戏');
INSERT INTO `type` VALUES (3, '生活', '生活');
INSERT INTO `type` VALUES (4, '美女', '美女');
INSERT INTO `type` VALUES (5, '风景', '风景');
INSERT INTO `type` VALUES (6, '动漫', '动漫');
INSERT INTO `type` VALUES (7, '学习', '学习');
INSERT INTO `type` VALUES (8, '趣事', '趣事');

-- ----------------------------
-- Table structure for word
-- ----------------------------
DROP TABLE IF EXISTS `word`;
CREATE TABLE `word`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '敏感词',
  `info` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '说明',
  PRIMARY KEY (`id` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of word
-- ----------------------------
INSERT INTO `word` VALUES (1, '傻逼', '骂人');

SET FOREIGN_KEY_CHECKS = 1;
