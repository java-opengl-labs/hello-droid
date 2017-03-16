package org.helloTriangle_es2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.jogamp.newt.event.MonitorEvent;
import com.jogamp.newt.event.MonitorModeListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import jogamp.newt.driver.android.NewtBaseActivity;
import org.helloDroid.Render_;

public class MainActivity extends NewtBaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        GLProfile profile = GLProfile.get(GLProfile.GLES2);
        GLCapabilities caps = new GLCapabilities(profile);
        GLWindow window = GLWindow.create(caps);
        window.setFullscreen(true);

        setContentView(getWindow(), window);

        window.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (e.getPressure(true) > 2f) { // show Keyboard
                    ((com.jogamp.newt.Window) e.getSource()).setKeyboardVisible(true);
                }
            }
        });

//        final Render render = new Render(this);
        final Render_ render = new Render_(this);

        // demo.enableAndroidTrace(true);
        window.addGLEventListener(render);
        window.getScreen().addMonitorModeListener(new MonitorModeListener() {
            public void monitorModeChangeNotify(MonitorEvent me) {
            }
            public void monitorModeChanged(MonitorEvent me, boolean success) {
                System.err.println("ScreenMode Changed: " + me);
            }
        });

        Animator animator = new Animator(window);
        //setAnimator(animator);

        window.setVisible(true);
        animator.setUpdateFPSFrames(60, System.err);
        animator.resetFPSCounter();
        window.resetFPSCounter();
    }
}
