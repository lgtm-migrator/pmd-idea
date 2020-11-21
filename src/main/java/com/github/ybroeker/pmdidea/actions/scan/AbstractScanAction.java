package com.github.ybroeker.pmdidea.actions.scan;

import java.io.File;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;

import com.github.ybroeker.pmdidea.config.PmdConfigurationService;
import com.github.ybroeker.pmdidea.pmd.*;
import com.github.ybroeker.pmdidea.toolwindow.PmdToolPanel;
import com.github.ybroeker.pmdidea.toolwindow.PmdToolWindowFactory;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class AbstractScanAction extends AnAction {

    protected abstract List<File> getFiles(@NotNull final Project project);

    @Override
    public void update(@NotNull final AnActionEvent event) {
        final Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

        final Optional<Path> rules = getRules(project);

        final Presentation presentation = event.getPresentation();
        if (rules.isPresent() != presentation.isEnabled()) {
            presentation.setEnabled(rules.isPresent());
        }
    }

    private Optional<Path> getRules(final Project project) {
        final PmdConfigurationService service = project.getService(PmdConfigurationService.class);
        final String pathName = service.getState().getRulesPath();
        if (pathName == null || pathName.isEmpty()) {
            return Optional.empty();
        }
        final Path rulesPath = Paths.get(pathName);
        if (!Files.exists(rulesPath)) {
            return Optional.empty();
        }
        return Optional.of(rulesPath);
    }

    @Override
    public final void actionPerformed(final AnActionEvent event) {
        final Project project = event.getData(CommonDataKeys.PROJECT);
        if (project == null) {
            return;
        }

        final PmdConfigurationService service = project.getService(PmdConfigurationService.class);

        final Optional<Path> rulesPath = getRules(project);
        if (!rulesPath.isPresent()) {
            return;
        }

        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(PmdToolWindowFactory.TOOL_WINDOW_ID);

        final PmdToolPanel toolPanel = (PmdToolPanel) toolWindow.getContentManager().getContent(0).getComponent();

        final List<File> files = getFiles(project);

        final PmdRunListener pmdRunListener = new PmdRunListenerAdapter(toolPanel);

        PmdAdapter pmdAdapter = project.getService(PmdAdapterDelegate.class);

        final PmdOptions pmdOptions = new PmdOptions(service.getState().getJdkVersion().toString(), service.getState().getPmdVersion());
        PmdConfiguration configuration = new PmdConfiguration(project, files, rulesPath.get().toFile().getAbsolutePath(), pmdOptions, pmdRunListener);

        ApplicationManager.getApplication().saveAll();
        ApplicationManager.getApplication().runReadAction(() -> {
            ApplicationManager.getApplication().executeOnPooledThread(() -> pmdAdapter.runPmd(configuration));
        });
    }


}
