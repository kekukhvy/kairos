package dev.kairos.admin.feature.task;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import dev.kairos.admin.shared.layout.MainLayout;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;

@Route(value = TaskRoutes.TASKS, layout = MainLayout.class)
@RouteAlias(value = TaskRoutes.ROOT, layout = MainLayout.class)
@PageTitle(TaskRoutes.PAGE_TITLE)
public class TaskView extends VerticalLayout {

    public TaskView() {

        setSizeFull();
        setSpacing(false);
        setPadding(false);


        StyleConfig.create()
                .padding(Tokens.SPACE_L)
                .gap(Tokens.SPACE_M)
                .applyTo(this);

        add(createTitle());
    }


    private H2 createTitle() {
        H2 title = new H2(TaskText.TITLE);
        return StyleConfig.create()
                .fontSize(Tokens.FONT_XL)
                .applyTo(title);
    }
}