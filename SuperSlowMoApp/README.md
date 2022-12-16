#SuperSlowMoApp

SuperSlowMoApp is an Android application that allows you to create slow-motion videos without needing fancy cameras with high framerates. 

The project is based on a paper published by NVidia (H. Jiang et al.) which makes use of a convolutional neural network to achieve frame interpolation. The project makes use of an existing PyTorch model which implements such CNN. Links are available below.

[[Paper](https://arxiv.org/abs/1712.00080)] [[Original project](http://jianghz.me/projects/superslomo/)]

[[Pytorch model](https://github.com/avinashpaliwal/Super-SloMo)]

Due to computational limits, the current (alpha) version limits the output resolution to 320x180 pixels. 

Authors:

- Filippo Lenzi ([@filloax](https://github.com/filloax))
- Guglielmo Palaferri ([@gullpet](https://github.com/gullpet))