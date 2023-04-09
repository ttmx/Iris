#version 150 core

in vec3 iris_Position;

void main() {
    gl_Position = mat4(2.0, 0.0, 0.0, 0.0, 0.0, 2.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, -1.0, 0.0, 1.0) * vec4(iris_Position, 1.0);
}
