@echo off
title 基础服务启动脚本
color 0A

echo 正在启动 Consul...
start "Consul" /D "D:\consul_1.21.0_windows_amd64" cmd /k "consul agent -dev"

echo 正在启动 MinIO...
start "MinIO" /D "D:\minio" cmd /k "minio.exe server D:\minio\data --console-address 127.0.0.1:9090 --address 127.0.0.1:9000"

echo 正在启动 Redis...
start "Redis" /D "D:\Redis-x64-5.0.14.1" cmd /k "redis-server.exe redis.windows.conf"

echo 正在启动 Elasticsearch...
start "Elasticsearch" /D "D:\elasticsearch-7.4.2\bin" cmd /k "elasticsearch.bat"

echo 所有服务已尝试启动。
pause