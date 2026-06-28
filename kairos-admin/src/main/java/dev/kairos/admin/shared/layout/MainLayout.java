package dev.kairos.admin.shared.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import dev.kairos.admin.feature.task.TaskView;
import dev.kairos.admin.shared.style.StyleConfig;
import dev.kairos.admin.shared.style.Tokens;

public class MainLayout extends AppLayout {

    private static final String APP_NAME = "Kairos";

    public MainLayout() {
        setPrimarySection(Section.NAVBAR);
        addToNavbar(new DrawerToggle(), createTitle());
        addToDrawer(createSideNav());
    }

    private Span createTitle() {
        Span title = new Span(LayoutText.APP_NAME);

        return StyleConfig.create()
                .fontSize(Tokens.FONT_L)
                .fontWeight(Tokens.FONT_WEIGHT_SEMIBOLD)
                .color(Tokens.TEXT_BODY)
                .marginInline(Tokens.SPACE_S)
                .applyTo(title);
    }

    private SideNav createSideNav() {
        SideNav nav = new SideNav();
        nav.addItem(new SideNavItem(
                LayoutText.NAV_TASKS,
                TaskView.class,
                VaadinIcon.TASKS.create()
        ));
        return nav;
    }

}