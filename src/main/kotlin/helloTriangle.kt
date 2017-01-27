import buffer.destroy
import com.jogamp.common.net.Uri
import com.jogamp.newt.event.KeyEvent
import com.jogamp.newt.event.KeyListener
import com.jogamp.newt.opengl.GLWindow
import com.jogamp.opengl.*
import com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER
import com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER
import com.jogamp.opengl.GL2ES3.*
import com.jogamp.opengl.GL4.GL_MAP_COHERENT_BIT
import com.jogamp.opengl.GL4.GL_MAP_PERSISTENT_BIT
import com.jogamp.opengl.math.FloatUtil
import com.jogamp.opengl.util.Animator
import com.jogamp.opengl.util.GLBuffers
import com.jogamp.opengl.util.glsl.ShaderCode
import com.jogamp.opengl.util.glsl.ShaderProgram
import extensions.name
import mat.Mat4
import vec._2.Vec2
import vec._3.Vec3
import java.nio.ByteBuffer


/**
 * Created by GBarbieri on 17.01.2017.
 */

fun main(args: Array<String>) {

    val helloTriangle = HelloTriangle()

    with(window) {

        setSize(1024, 768)
        setPosition(100, 50)
        title = "Hello Triangle"

        addGLEventListener(helloTriangle)
        addKeyListener(helloTriangle)

        isVisible = true
    }
    animator.start()
}

val window = GLWindow.create(GLCapabilities(GLProfile.get(GLProfile.GL4)))
val animator = Animator(window)

class HelloTriangle : GLEventListener, KeyListener {

    object Buffer {
        val VERTEX = 0
        val ELEMENT = 1
        val TRANSFORM = 2
        val MAX = 3
    }

    object Semantic {
        object Attr {
            val POSITION = 0
            val COLOR = 3
        }

        object Stream {
            val _0 = 0
        }

        object Uniform {
            val TRANSFORM0 = 1
        }
    }

    val bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX)
    val vertexArrayName = GLBuffers.newDirectIntBuffer(1)

    val shaderSrc = "hello-triangle"

    lateinit var program:ShaderProgram

    lateinit var transformPointer: ByteBuffer

    var start = 0L

    var clearColor = GLBuffers.newDirectFloatBuffer(floatArrayOf(1f, .5f, 0f, 1f))
    var clearDepth = GLBuffers.newDirectFloatBuffer(floatArrayOf(1f))

    override fun init(drawable: GLAutoDrawable) {

        val gl4 = drawable.gl.gL4

        initBuffers(gl4)

        initVertexArray(gl4)

        program = ShaderProgram.create(gl4, shaderSrc)

        // map the transform buffer and keep it mapped
        with(gl4) {
            transformPointer = glMapNamedBufferRange(
                    bufferName[Buffer.TRANSFORM], // buffer
                    0, // offset
                    Mat4.SIZE.toLong(), // size
                    GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT or GL_MAP_INVALIDATE_BUFFER_BIT) // flags

            glEnable(GL_DEPTH_TEST)
        }

        start = System.currentTimeMillis()
    }

    fun initBuffers(gl4: GL4) {

        val bug1287 = true

        val vertexBuffer = GLBuffers.newDirectFloatBuffer(floatArrayOf(
                -1f, -1f, /**/ 1f, 0f, 0f,
                +0f, +2f, /**/ 0f, 0f, 1f,
                +1f, -1f, /**/ 0f, 1f, 0f))
        val elementBuffer = GLBuffers.newDirectShortBuffer(shortArrayOf(0, 2, 1))

        with(gl4) {

            glCreateBuffers(Buffer.MAX, bufferName)

            if (!bug1287) {

                glNamedBufferStorage(bufferName[Buffer.VERTEX], vertexBuffer.SIZE.L, vertexBuffer, GL_STATIC_DRAW)

                glNamedBufferStorage(bufferName[Buffer.ELEMENT], (elementBuffer.capacity() * Short.BYTES).L, elementBuffer, GL_STATIC_DRAW)

                glNamedBufferStorage(bufferName[Buffer.TRANSFORM], (16 * Float.BYTES).L, null, GL_MAP_WRITE_BIT)

            } else {

                // vertices
                glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX])
                glBufferStorage(GL_ARRAY_BUFFER, (vertexBuffer.capacity() * Float.BYTES).toLong(), vertexBuffer, 0)
                glBindBuffer(GL_ARRAY_BUFFER, 0);

                // elements
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT])
                glBufferStorage(GL_ELEMENT_ARRAY_BUFFER, (elementBuffer.capacity() * Short.BYTES).L, elementBuffer, 0)
                glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)

                // transform
                glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM])

                val uniformBufferOffset = GLBuffers.newDirectIntBuffer(1)
                glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset)
                val uniformBlockSize = glm.max(Mat4.SIZE, uniformBufferOffset.get(0))

                glBufferStorage(GL_UNIFORM_BUFFER, uniformBlockSize.L, null,
                        GL_MAP_WRITE_BIT or GL_MAP_PERSISTENT_BIT or GL_MAP_COHERENT_BIT)

                uniformBufferOffset.destroy()

                glBindBuffer(GL_UNIFORM_BUFFER, 0)
            }
        }

        vertexBuffer.destroy()
        elementBuffer.destroy()
    }

    fun initVertexArray(gl4: GL4) {

        with(gl4) {

            glCreateVertexArrays(1, vertexArrayName)

            glVertexArrayAttribBinding(vertexArrayName[0], Semantic.Attr.POSITION, Semantic.Stream._0)
            glVertexArrayAttribBinding(vertexArrayName[0], Semantic.Attr.COLOR, Semantic.Stream._0)

            glVertexArrayAttribFormat(vertexArrayName[0], Semantic.Attr.POSITION, 2, GL_FLOAT, false, 0)
            glVertexArrayAttribFormat(vertexArrayName[0], Semantic.Attr.COLOR, 3, GL_FLOAT, false, Vec2.SIZE)

            glEnableVertexArrayAttrib(vertexArrayName[0], Semantic.Attr.POSITION)
            glEnableVertexArrayAttrib(vertexArrayName[0], Semantic.Attr.COLOR)

            glVertexArrayElementBuffer(vertexArrayName[0], bufferName.get(Buffer.ELEMENT))
            glVertexArrayVertexBuffer(vertexArrayName[0], Semantic.Stream._0, bufferName.get(Buffer.VERTEX), 0, Vec2.SIZE + Vec3.SIZE)
        }
    }

    fun initProgram(gl4: GL4) {

        val vertShader = ShaderCode.create(gl4, GL_VERTEX_SHADER, 1, javaClass, arrayOf("$shaderSrc.vert"), false)
        val fragShader = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, 1, javaClass, arrayOf("$shaderSrc.frag"), false)

        program.add(vertShader)
        program.add(fragShader)

        program.init(gl4)

        program.link(gl4, System.out)

        vertShader.destroy(gl4)
        fragShader.destroy(gl4)
    }

    override fun display(drawable: GLAutoDrawable) {

        with(drawable.gl.gL4) {

            glClearBufferfv(GL_COLOR, 0, clearColor)
            glClearBufferfv(GL_DEPTH, 0, clearDepth)

            run {
                // update matrix based on time
                val diff = (System.currentTimeMillis() - start) / 1_000f
                /**
                 * Here we build the matrix that will multiply our original vertex
                 * positions. We scale (halving it) and rotate it around Z coordinate.
                 */
                val scale = FloatUtil.makeScale(FloatArray(16), true, 0.5f, 0.5f, 0.5f);
                val zRotazion = FloatUtil.makeRotationEuler(FloatArray(16), 0, 0f, 0f, diff);
                val modelToClip = FloatUtil.multMatrix(scale, zRotazion);

                transformPointer.asFloatBuffer().put(modelToClip);
            }
            glUseProgram(program.name)
            glBindVertexArray(vertexArrayName[0])

            glBindBufferBase(
                    GL_UNIFORM_BUFFER, // Target
                    Semantic.Uniform.TRANSFORM0, // index
                    bufferName.get(Buffer.TRANSFORM)) // buffer

            glDrawElements(
                    GL_TRIANGLES, // primitive mode
                    3, // element count
                    GL_UNSIGNED_SHORT, // element type
                    0) // element offset}

            glBindVertexArray(0)
        }
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        with(drawable.gl.gL4) {
            glViewport(x, y, width, height)
        }
    }

    override fun dispose(drawable: GLAutoDrawable) {

        with(drawable.gl.gL4) {

            glUnmapNamedBuffer(bufferName.get(Buffer.TRANSFORM))

            glDeleteVertexArrays(1, vertexArrayName)
            glDeleteBuffers(Buffer.MAX, bufferName)

            program.destroy(this)
        }

        vertexArrayName.destroy()
        bufferName.destroy()

        clearColor.destroy()
        clearDepth.destroy()

        System.exit(0)
    }

    override fun keyPressed(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_ESCAPE -> {
                animator.remove(window)
                window.destroy()
            }
        }
    }

    override fun keyReleased(e: KeyEvent) {}
}