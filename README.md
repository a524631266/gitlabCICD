[TOC]
# 本项目为gitlab shared runner运行

测试 gitlab-runner的过程

# gitlab CI/CD 自动构建原理

## 创建runner的执行环境
    
    此环境是用来执行runner所需要的环境
    安装环境如下
    1. 本地系统（非docker环境）
    2. docker中
    3. k8s三种环境

本篇使用docker环境，安装runner，隔离环境，方便部署

## 1. gitlab 运行命令 查看gitlab配置
统一配置文件，找到gitlab的config，并在gitlab中新建 gitlab-runner文件
```
#  docker run --detach --hostname 192.168.1.163 --publish 443:443 --publish 80:80 --publish 2222:22 --name gitlab --restart always --volume /home/gitlab/config:/etc/gitlab --vo
lume /home/gitlab/logs:/var/log/gitlab --volume /home/gitlab/data:/var/opt/gitlab gitlab/gitlab-ee:latest
```
## 2. gitlabrunner  执行一个可以运行runner的docker容器
runner配置文件放在 gitlab/gitlab-runner/config目录中
```
$ sudo docker run -d --name gitlab-runner --restart always \
  -v /home/gitlab/gitlab-runner/config:/etc/gitlab-runner \
  -v /var/run/docker.sock:/var/run/docker.sock \
  gitlab/gitlab-runner:latest


```
## 3.进入 gitlab-runner 内部构建mvn docker 以及其他文件， 用来生成一个镜像

```
$ sudo docker exec -it gitlab-runner /bin/bash
```

### 3.1 两种方式创建executor

#### a) 进入gitlab-runner容器 手动根据提示按步骤创建容器，不能挂载，只能通过配置文件修改
```
# gitlab-runner register
Runtime platform                                    arch=amd64 os=linux pid=1999 revision=4c96e5ad version=12.9.0
Running in system-mode.                            
                                                   
$ Please enter the gitlab-ci coordinator URL (e.g. https://gitlab.com/):
http://192.168.1.163/
$ Please enter the gitlab-ci token for this runner:
ajsdjflajsldjflasjdlf
$ Please enter the gitlab-ci description for this runner:
[a019bb289bd5]: test for docker quick
$ Please enter the gitlab-ci tags for this runner (comma separated):
$ 
$ Registering runner... succeeded                     runner=ajsdjflaj
$ Please enter the executor: parallels, ssh, virtualbox, shell, docker+machine, docker-ssh+machine, kubernetes, custo
$ m, docker, docker-ssh:docker
$ Please enter the default Docker image (e.g. ruby:2.6):
192.168.1.163/maven:3.6.2
$ Runner registered successfully. Feel free to start it, but if it's running already the config should be automatical
$ ly reloaded! 
```

##### 创建好之后，为了让子镜像能够执行宿主容器中的docker命令
在gitlab runner配置文件中新增 volumes 字段，并配合引进docker
```text
[[runners]]
  name = "test for docker quick"
  url = "http://192.168.1.163/"
  token = "asdfasdfasdfasdf"
  executor = "docker"
  [runners.custom_build_dir]
  [runners.cache]
    [runners.cache.s3]
    [runners.cache.gcs]
  [runners.docker]
    tls_verify = false
    image = "192.168.1.163/maven:3.6.2"
    privileged = false
    disable_entrypoint_overwrite = false
    oom_kill_disable = false
    disable_cache = false
    volumes = ["/cache","/root/.docker/config.json:/root/.docker/config.json","/var/run/docker.sock:/var/run/docker.sock","/root/.m2:/root/.m2","/usr/bin/docker:/usr/bin/docker"]
    shm_size = 0

```

#### b) 或者使用gitlab-ci-multi-runner命令创建，同上
```shell
# gitlab-ci-multi-runner register -n \
  --url http://192.168.1.163/ \
  --registration-token asdfasdfasdfasdfasdf \
  --description "has no docker shell in enviroment standard CI/CD build by maven project and build to docker images and deploy to k8s cluster" \
  --docker-privileged=true \
  --docker-pull-policy="if-not-present" \
  --docker-image "192.168.1.163/maven_for_docker:3.6.2" \
  --docker-volumes /root/.docker/config.json:/root/.docker/config.json \
  --docker-volumes /var/run/docker.sock:/var/run/docker.sock \
  --docker-volumes /root/.m2:/root/.m2 \
  --docker-volumes /usr/bin/docker:/usr/bin/docker \
  --executor docker
```
注 :
    给executor达标亲
    --tag-list=dev,uat,prod \
## 4 创建镜像
### 4.1创建一个支持kubectl命令的容器

配置文件： Dockerfile_kubectl 以及 config文件
[详情请看](http://www.imooc.com/article/293003)

```
$ sudo docker build --build-arg KUBE_LATEST_VERSION="v1.13.6" -t 192.168.1.163/imooc/kubectl:1.13.6.1 -f Dockerfile_kubectl.
<!-- 查看是否可用 -->
$ sudo docker run -it 192.168.1.163/imooc/kubectl:1.13.6.1  /bin/sh

$ sudo docker push 192.168.1.163/maven_for_docker:3.6.2
```
### 4.2创建一个maven仓库

dockerfile 请看 Dockerfile_maven
```
sudo docker build --build-arg KUBE_LATEST_VERSION="v1.13.6" -t 192.168.1.163/imooc/kubectl:1.13.6.1 -f Dockerfile_maven.
```

## 5. 核心自动部署文件 .gitlab-ci.yml
请查看当前文件目录中的.gitlab-ci.yml
