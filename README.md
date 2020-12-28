# Alluxio-DataPlacement

​	本项目包括Alluxio集群core/server策略和节点内数据布局策略。主要模块代码在loadbalance，core/client，core/server，python目录下：

- loadbalance目录下的代码为纠删编解码、读写代理和测试负载。
- core/server目录的相应common、master、worker文件夹下有前端代码。
- core/client目录相应的policy文件夹下为节点间布局自带策略和优化策略模块。
- core/server/.../worker/block目录下为节点内布局相关优化机制及策略代码。
- python目录下为负载生成及实验代码。

编译运行方法：

- 需要提前安装配置isa-l库。
- 运行主目录下的rebuild.sh脚本，即可编译。
- 运行bin目录下的alluxio-start.sh [all NoMount/Mount]和alluxio-stop.sh [all]脚本即可启动或关闭Alluxio集群。
- 其他问题详见注意事项。

​	

