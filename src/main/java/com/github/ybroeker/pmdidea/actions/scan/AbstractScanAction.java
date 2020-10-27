package com.github.ybroeker.pmdidea.actions.scan;

import java.io.File;
import java.nio.file.*;
import java.util.List;

import com.github.ybroeker.pmdidea.pmd.PmdRunListener;
import com.github.ybroeker.pmdidea.pmd.PmdRunner;
import com.github.ybroeker.pmdidea.toolwindow.PmdToolPanel;
import com.github.ybroeker.pmdidea.toolwindow.PmdToolWindowFactory;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;


public abstract class AbstractScanAction extends AnAction {

    protected abstract List<File> getFiles(@NotNull final Project project);

    @Override
    public final void actionPerformed(final AnActionEvent event) {
        final Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

        final Path path = Paths.get(project.getBasePath(), "pmd-rules.xml");
        if (!Files.exists(path)) {
            return;
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PmdToolWindowFactory.TOOL_WINDOW_ID);

        final PmdToolPanel scan = (PmdToolPanel) toolWindow.getContentManager().getContent(0).getComponent();

        final List<File> files = getFiles(project);

        final PmdRunListener pmdRunListener = new PmdRunListenerAdapter(scan);

        //TODO: Select correct rule-set


        final PmdRunner pmdRunner = new PmdRunner(project, files, path.toFile().getAbsolutePath(), pmdRunListener);
        ApplicationManager.getApplication().saveAll();
        ApplicationManager.getApplication().runReadAction(() -> {
            ApplicationManager.getApplication().executeOnPooledThread(pmdRunner);
        });
    }


}
