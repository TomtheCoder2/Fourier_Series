let original_functionX = [];
let fourier;

let time = 0;
let wave = [];
let path = [];
let targetWave = [];

let slider;

function setup() {
    createCanvas(800, 600);
    const skip = 10;
    for (let i = 0; i < drawing.length; i+=skip) {
        const c = new Complex(drawing[i].x, drawing[i].y);
        original_functionX.push(c);
    }
    fourier = dft(original_functionX);
    fourier.sort((a, b) => b.amp - a.amp);
}

function epiCycles(x, y, rotation, fourier) {
    for (let i = 0; i < fourier.length; i++) {
        let prevX = x;
        let prevY = y;

        let freq = fourier[i].freq;
        let radius = fourier[i].amp;
        let phase = fourier[i].phase;
        x += radius * cos(freq * time + phase + rotation);
        y += radius * sin(freq * time + phase + rotation);

        stroke(255, 100);
        noFill();
        ellipse(prevX, prevY, radius * 2);

        fill(255);
        stroke(255);
        line(prevX, prevY, x, y);
    }

    return createVector(x, y);
}

function draw() {
    background(0);

    let v = epiCycles(width / 2, height / 2, 0, fourier);
    path.unshift(v);
       // line(vx.x, vx.y, v.x, v.y);
    // line(vy.x, vy.y, v.x, v.y);
    beginShape();
    noFill();
    for (let i = 0; i < path.length; i++) {
        vertex(path[i].x, path[i].y);
    }
    endShape()

    if (time > TWO_PI) {
        time = 0;
        path = [];
    }


    const dt = TWO_PI / fourier.length;
    time += dt;
}