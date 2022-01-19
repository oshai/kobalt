# Kobalt

Kobalt is a universal build system.

# Getting Started

## Install Kobalt
### Manually
1. Download [zip file](https://github.com/oshai/kobalt/releases/latest) (ie `kobalt-1.1.7.zip).
2. Run from terminal:
```bash
mkdir -p ~/kobalt
cd yourLocation
unzip kobalt-1.1.7.zip
mv kobalt-1.1.7 ~/kobalt/
export PATH=~/kobalt/kobalt-1.1.7/bin:$PATH
chmod +x ~/kobalt/kobalt-1.1.7/bin/kobaltw
# also need it in cache ATM, until download is fixed
mkdir -p ~/.kobalt/wrapper/dist
cp kobalt-1.1.7.zip ~/.kobalt/wrapper/dist/
```

## Initialize your project

Change to your project directory and call the kobaltw command with --init:
- To initialize a Java project:
```
cd ~/java/project
kobaltw --init java
```
- To initialize a Kotlin project:
```
cd ~/kotlin/project
kobaltw --init kotlin
```

# References

- beust docs: https://beust.com/kobalt/home/index.html
- 
# Contribution 
To build it:

```
$ ./kobaltw assemble
```

Please see [the web site](http://beust.com/kobalt/) for the full documentation.

