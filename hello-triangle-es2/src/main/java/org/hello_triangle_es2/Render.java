package org.hello_triangle_es2;

import android.content.Context;
import android.content.ContextWrapper;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static com.jogamp.opengl.GL.*;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;


/**
 * Created by elect on 15/03/17.
 */
public class Render extends ContextWrapper implements GLEventListener {

    public Render(Context base) {
        super(base);
    }

    private float[] vertexData = {
            -0.5f, -0.5f, 1, 0, 0,
            +0.0f, +1.0f, 0, 0, 1,
            +0.5f, -0.5f, 0, 1, 0};

    private short[] elementData = {0, 2, 1};

    private interface Buffer {

        int VERTEX = 0;
        int ELEMENT = 1;
        int MAX = 3;
    }

    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
    private IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);

    private FloatBuffer clearColor = GLBuffers.newDirectFloatBuffer(4);
    private FloatBuffer clearDepth = GLBuffers.newDirectFloatBuffer(1);

    private FloatBuffer matBuffer = GLBuffers.newDirectFloatBuffer(16);

    private Program program;

    private long start;

    @Override
    public void init(GLAutoDrawable drawable) {

        GL2ES2 gl = drawable.getGL().getGL2ES2();

        initProgram(gl);

        initBuffers(gl);

        start = System.currentTimeMillis();
    }


    private void initProgram(GL2ES2 gl) {

        program = new Program(gl, "hello-triangle.vert", "hello-triangle.frag");

        Semantic.Attr.POSITION = gl.glGetAttribLocation(program.name, "position");
        Semantic.Attr.COLOR = gl.glGetAttribLocation(program.name, "color");

        checkError(gl, "initProgram");
    }

    private void initBuffers(GL2ES2 gl) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);

        gl.glGenBuffers(Buffer.MAX, bufferName);

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4, vertexBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementBuffer.capacity() * 2, elementBuffer, GL_STATIC_DRAW);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        checkError(gl, "initBuffers");
    }

    @Override
    public void display(GLAutoDrawable drawable) {

        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glClearColor(1f, .66f, 0.33f, 1f);
        gl.glClearDepth(1f);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        gl.glUseProgram(program.name);

        // view matrix
        {
            float[] view = new float[16];
            FloatUtil.makeIdentity(view);

            matBuffer.put(view).position(0);

            gl.glUniformMatrix4fv(program.viewUL, 1, false, matBuffer);
        }

        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        {
            int stride = (2 + 3) * 4;
            int offset = 0;

            gl.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, stride, offset);

            offset = 2 * 4;
            gl.glEnableVertexAttribArray(Semantic.Attr.COLOR);
            gl.glVertexAttribPointer(Semantic.Attr.COLOR, 3, GL_FLOAT, false, stride, offset);
        }
        gl.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));

        // model matrix
        {
            long now = System.currentTimeMillis();
            float diff = (float) (now - start) / 1_000f;

            float[] model = FloatUtil.makeRotationEuler(new float[16], 0, 0, 0, diff);
            matBuffer.put(model).position(0);

            gl.glUniformMatrix4fv(program.modelUL, 1, false, matBuffer);
        }

        gl.glDrawElements(GL_TRIANGLES, elementData.length, GL_UNSIGNED_SHORT, 0);

        gl.glDisableVertexAttribArray(Semantic.Attr.POSITION);
        gl.glDisableVertexAttribArray(Semantic.Attr.COLOR);
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        gl.glUseProgram(0);

        checkError(gl, "display");
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

        GL2ES2 gl = drawable.getGL().getGL2ES2();

        float[] ortho = new float[16];
        FloatUtil.makeOrtho(ortho, 0, false, -1, 1, -1, 1, 1, -1);
        matBuffer.put(ortho).position(0);

        gl.glUseProgram(program.name);
        gl.glUniformMatrix4fv(program.projUL, 1, false, matBuffer);
        gl.glUseProgram(0);

        gl.glViewport(x, y, width, height);
    }


    @Override
    public void dispose(GLAutoDrawable drawable) {

        GL2ES2 gl = drawable.getGL().getGL2ES2();

        gl.glDeleteProgram(program.name);
        gl.glDeleteBuffers(Buffer.MAX, bufferName);
    }


    private class Program {

        int name, modelUL, viewUL, projUL;

        Program(GL2ES2 gl, String vertex, String fragment) {

            ShaderCode vertShader = new ShaderCode(GL_VERTEX_SHADER, 1, getShader(vertex));
            ShaderCode fragShader = new ShaderCode(GL_FRAGMENT_SHADER, 1, getShader(fragment));

            ShaderProgram shaderProgram = new ShaderProgram();

            shaderProgram.add(vertShader);
            shaderProgram.add(fragShader);

            shaderProgram.init(gl);

            name = shaderProgram.program();

            shaderProgram.link(gl, System.err);

            modelUL = getUniformLocation(gl, name, "model");
            viewUL = getUniformLocation(gl, name, "view");
            projUL = getUniformLocation(gl, name, "proj");
        }

        private int getUniformLocation(GL2ES2 gl, int program, String name) {

            int location = gl.glGetUniformLocation(program, name);

            if (location == -1) {
                System.err.println("uniform" + name + " not found!");
            }
            return location;
        }

        private CharSequence[][] getShader(int id) {

            InputStream inputStream = getResources().openRawResource(id);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();

            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line + '\n');
                    line = bufferedReader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return new CharSequence[][]{{stringBuilder.toString()}};
        }

        private CharSequence[][] getShader(String shader) {

            InputStream inputStream = null;
            try {
                inputStream = getAssets().open(shader);
            } catch (IOException e) {
                e.printStackTrace();
            }

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();

            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    stringBuilder.append(line + '\n');
                    line = bufferedReader.readLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    inputStream.close();
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return new CharSequence[][]{{stringBuilder.toString()}};
        }
    }

    private void checkError(GL2ES2 gl, String location) {

        int error = gl.glGetError();
        if (error != GL_NO_ERROR) {
            String errorString;
            switch (error) {
                case GL_INVALID_ENUM:
                    errorString = "GL_INVALID_ENUM";
                    break;
                case GL_INVALID_VALUE:
                    errorString = "GL_INVALID_VALUE";
                    break;
                case GL_INVALID_OPERATION:
                    errorString = "GL_INVALID_OPERATION";
                    break;
                case GL_INVALID_FRAMEBUFFER_OPERATION:
                    errorString = "GL_INVALID_FRAMEBUFFER_OPERATION";
                    break;
                case GL_OUT_OF_MEMORY:
                    errorString = "GL_OUT_OF_MEMORY";
                    break;
                default:
                    errorString = "UNKNOWN";
                    break;
            }
            throw new Error("OpenGL Error(" + errorString + "): " + location);
        }
    }
}
