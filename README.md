# SinoBili

- 使用 Java 在命令行获取 Bilibili 的信息.

## 特性

```text
video <aid | bvid>        - get video info
wbi                       - update wbi sign keys
biliticket [csrf]         - get bili ticket, 'csrf' is optional
netdisk <file> <cookies>  - upload file to Bilibili as netdisk
sharelink <link>          - download file from SSB share link
clear                     - clear screen
help                      - show help
exit                      - exit program
```

## 编译

您可以在 Releases 下载构建好的 JAR 文件，也可以自己编译

请先确保至少安装了 GNU Make 与 JDK 8 或以上版本，然后运行以下命令

```shell
make
```

编译完成后，会在项目根目录下 `build` 目录生成 `sinobili.jar` 文件

## 运行

假设当前目录下存在文件名为 `sinobili.jar`，则运行以下命令

```shell
java -jar sinobili.jar
```
