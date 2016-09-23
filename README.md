# K-Sonic
Based on Sonic (speed , pitch and rate) , the demo for Android

![](https://raw.githubusercontent.com/jcodeing/K-Sonic/master/lookme.gif)

Introduction
============
	一个基于Sonic(声音处理算法)的,音频Speed,Pitch,Rate调节Demo
	该Demo支持音频打开方式后直接可进行相关音频参数的调节,
	或者手动选择本地音频,或者直接播放本Demo内置音频,
	想要播放在线音频可手动修改源码音频Uri
Features
========
    该Demo支持两种媒体引擎,都支持音频的变速功能.
    一个是,基于Exo和Sonic Java算法
    另一个是,基于Presto和Sonic C算法
    -----------
    另外界面部分中
    含有一个PlusMinusNum自定义控件
    支持数字的累加/递减(float/int:需要自己再修改)
    支持长按连续累加/递减.....
Usage
=====
    直接引用K-Sonic项目中的library
    没必要引用两个
    你可以根据自己的需求进行选择
    library-exo: 处了是在java层进行音频变速外,其他音频播放和处理等一些操作,我还没有进行测试...
    library-presto: 如果没什么特殊情况,还是建议用这个的,兼容方面我已经做了很多完善,而且也是直接调用ndk,C算法相对效率会有点优势...
License
=======
    MIT License

    Copyright (c) 2016 Jcodeing <jcodeing@gmail.com>

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

