FROM nginx
# 复制编译后的静态文件到Nginx的默认站点目录
COPY ./dist /usr/share/nginx/html/
# 复制自定义Nginx配置（可选，若无则用默认配置）
# COPY nginx.conf /etc/nginx/conf.d/default.conf
# 暴露80端口
EXPOSE 80