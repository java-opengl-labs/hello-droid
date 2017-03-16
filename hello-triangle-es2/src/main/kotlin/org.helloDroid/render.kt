package org.helloDroid

import android.content.Context
import android.content.ContextWrapper
import com.jogamp.opengl.GL.*
import com.jogamp.opengl.GL2ES2
import com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER
import com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.math.FloatUtil
import com.jogamp.opengl.util.GLBuffers
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram
import org.hello_triangle_es2.Semantic
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.FloatBuffer
import java.nio.IntBuffer
import kotlin.properties.Delegates

/**
 * Created by elect on 15/03/17.
 */

class Render_(base: Context) : ContextWrapper(base), GLEventListener {

    val vertexData = floatArrayOf(
            -0.5f, -0.5f, 1f, 0f, 0f,
            +0.0f, +1.0f, 0f, 0f, 1f,
            +0.5f, -0.5f, 0f, 1f, 0f)

    val elementData = shortArrayOf(0, 2, 1)

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val MAX = 3
    }

    val bufferName: IntBuffer = GLBuffers.newDirectIntBuffer(Buffer.MAX)
    val vertexArrayName: IntBuffer = GLBuffers.newDirectIntBuffer(1)

    val clearColor: FloatBuffer = GLBuffers.newDirectFloatBuffer(4)
    val clearDepth: FloatBuffer = GLBuffers.newDirectFloatBuffer(1)

    val matBuffer: FloatBuffer = GLBuffers.newDirectFloatBuffer(16)

    var program by Delegates.notNull<Program>()

    var start = 0L

    override fun init(drawable: GLAutoDrawable) {

        val gl = drawable.gl.gL2ES2

        initProgram(gl)

        initBuffers(gl)

        start = System.currentTimeMillis()
    }


    fun initProgram(gl: GL2ES2) = with(gl) {

        program = Program(gl, "hello-triangle.vert", "hello-triangle.frag")

        Semantic.Attr.POSITION = glGetAttribLocation(program.name, "position")
        Semantic.Attr.COLOR = glGetAttribLocation(program.name, "color")

        checkError(gl, "initProgram")
    }

    fun initBuffers(gl: GL2ES2) = with(gl) {

        val vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData)
        val elementBuffer = GLBuffers.newDirectShortBuffer(elementData)

        glGenBuffers(Buffer.MAX, bufferName)

        glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX))
        glBufferData(GL_ARRAY_BUFFER, (vertexBuffer.capacity() * 4).toLong(), vertexBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ARRAY_BUFFER, 0)

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT))
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, (elementBuffer.capacity() * 2).toLong(), elementBuffer, GL_STATIC_DRAW)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

        checkError(gl, "initBuffers")
    }

    override fun display(drawable: GLAutoDrawable) {

        val gl = drawable.gl.gL2ES2

        with(gl) {

            glClearColor(1f, .66f, 0.33f, 1f)
            glClearDepth(1.0)
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            glUseProgram(program.name)

            // view matrix
            run {
                val view = FloatArray(16)
                FloatUtil.makeIdentity(view)

                matBuffer.put(view).position(0)

                glUniformMatrix4fv(program.viewUL, 1, false, matBuffer)
            }

            glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX))
            run {
                val stride = (2 + 3) * 4
                var offset = 0

                glEnableVertexAttribArray(Semantic.Attr.POSITION)
                glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, stride, offset.toLong())

                offset = 2 * 4
                glEnableVertexAttribArray(Semantic.Attr.COLOR)
                glVertexAttribPointer(Semantic.Attr.COLOR, 3, GL_FLOAT, false, stride, offset.toLong())
            }
            glBindBuffer(GL_ARRAY_BUFFER, 0)

            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT))

            // model matrix
            run {
                val now = System.currentTimeMillis()
                val diff = (now - start).toFloat() / 1_000f

                val model = FloatUtil.makeRotationEuler(FloatArray(16), 0, 0f, 0f, diff)
                matBuffer.put(model).position(0)

                glUniformMatrix4fv(program.modelUL, 1, false, matBuffer)
            }

            glDrawElements(GL_TRIANGLES, elementData.size, GL_UNSIGNED_SHORT, 0)

            glDisableVertexAttribArray(Semantic.Attr.POSITION)
            glDisableVertexAttribArray(Semantic.Attr.COLOR)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
            glUseProgram(0)
        }

        checkError(gl, "display")
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) = with(drawable.gl.gL2ES2) {

        val ortho = FloatArray(16)
        FloatUtil.makeOrtho(ortho, 0, false, -1f, 1f, -1f, 1f, 1f, -1f)
        matBuffer.put(ortho).position(0)

        glUseProgram(program.name)
        glUniformMatrix4fv(program.projUL, 1, false, matBuffer)
        glUseProgram(0)

        glViewport(x, y, width, height)
    }


    override fun dispose(drawable: GLAutoDrawable) = with(drawable.gl.gL2ES2) {

        glDeleteProgram(program.name)
        glDeleteBuffers(Buffer.MAX, bufferName)
    }


    inner class Program constructor(gl: GL2ES2, vertex: String, fragment: String) {

        var name = 0
        var modelUL = 0
        var viewUL = 0
        var projUL = 0

        init {

            val vertShader = ShaderCode(GL_VERTEX_SHADER, 1, getShader(vertex))
            val fragShader = ShaderCode(GL_FRAGMENT_SHADER, 1, getShader(fragment))

            val shaderProgram = ShaderProgram()

            shaderProgram.add(vertShader)
            shaderProgram.add(fragShader)

            shaderProgram.init(gl)

            name = shaderProgram.program()

            shaderProgram.link(gl, System.err)

            modelUL = getUniformLocation(gl, name, "model")
            viewUL = getUniformLocation(gl, name, "view")
            projUL = getUniformLocation(gl, name, "proj")
        }

        fun getUniformLocation(gl: GL2ES2, program: Int, name: String): Int {

            val location = gl.glGetUniformLocation(program, name)

            if (location == -1) {
                System.err.println("uniform$name not found!")
            }
            return location
        }

        fun getShader(id: Int): Array<Array<CharSequence>> {

            val inputStream = resources.openRawResource(id)
            val bufferedReader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()

            var line: String? = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line + '\n')
                line = bufferedReader.readLine()
            }

            return arrayOf(arrayOf<CharSequence>(stringBuilder.toString()))
        }

        fun getShader(shader: String): Array<Array<CharSequence>> {

            val inputStream = assets.open(shader)

            val bufferedReader = BufferedReader(InputStreamReader(inputStream!!))
            val stringBuilder = StringBuilder()

            var line: String? = bufferedReader.readLine()
            while (line != null) {
                stringBuilder.append(line + '\n')
                line = bufferedReader.readLine()
            }

            return arrayOf(arrayOf<CharSequence>(stringBuilder.toString()))
        }
    }

    fun checkError(gl: GL2ES2, location: String) {

        val error = gl.glGetError()
        if (error != GL_NO_ERROR) {
            val errorString = when (error) {
                GL_INVALID_ENUM -> "GL_INVALID_ENUM"
                GL_INVALID_VALUE -> "GL_INVALID_VALUE"
                GL_INVALID_OPERATION -> "GL_INVALID_OPERATION"
                GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION"
                GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY"
                else -> "UNKNOWN"
            }
            throw Error("OpenGL Error($errorString): $location")
        }
    }
}
