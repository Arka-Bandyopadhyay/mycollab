/**
 * Copyright © MyCollab
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mycollab.module.project.view.settings;

import com.mycollab.common.i18n.GenericI18Enum;
import com.mycollab.common.i18n.OptionI18nEnum.StatusI18nEnum;
import com.mycollab.configuration.SiteConfiguration;
import com.mycollab.core.arguments.ValuedBean;
import com.mycollab.core.utils.BeanUtility;
import com.mycollab.module.project.CurrentProjectVariables;
import com.mycollab.module.project.ProjectRolePermissionCollections;
import com.mycollab.module.project.ProjectTypeConstants;
import com.mycollab.module.project.i18n.ComponentI18nEnum;
import com.mycollab.module.project.i18n.ProjectCommonI18nEnum;
import com.mycollab.module.project.ui.ProjectAssetsManager;
import com.mycollab.module.project.ui.components.*;
import com.mycollab.module.project.view.ProjectView;
import com.mycollab.module.project.domain.SimpleComponent;
import com.mycollab.module.project.service.ComponentService;
import com.mycollab.spring.AppContextUtil;
import com.mycollab.vaadin.UserUIContext;
import com.mycollab.vaadin.event.HasPreviewFormHandlers;
import com.mycollab.vaadin.mvp.ViewComponent;
import com.mycollab.vaadin.ui.UIUtils;
import com.mycollab.vaadin.web.ui.AbstractPreviewItemComp;
import com.mycollab.vaadin.web.ui.AdvancedPreviewBeanForm;
import com.mycollab.vaadin.web.ui.WebThemes;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.viritin.button.MButton;
import org.vaadin.viritin.layouts.MVerticalLayout;

/**
 * @author MyCollab Ltd.
 * @since 1.0
 */
@ViewComponent
public class ComponentReadViewImpl extends AbstractPreviewItemComp<SimpleComponent> implements ComponentReadView {
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(ComponentReadViewImpl.class);

    private ProjectActivityComponent activityComponent;
    private TagViewComponent tagViewComponent;
    private MButton quickActionStatusBtn;

    private DateInfoComp dateInfoComp;
    private PeopleInfoComp peopleInfoComp;
    private ComponentTimeLogComp componentTimeLogComp;

    public ComponentReadViewImpl() {
        super(UserUIContext.getMessage(ComponentI18nEnum.DETAIL),
                ProjectAssetsManager.getAsset(ProjectTypeConstants.COMPONENT));
    }

    @Override
    public SimpleComponent getItem() {
        return this.beanItem;
    }

    @Override
    public HasPreviewFormHandlers<SimpleComponent> getPreviewFormHandlers() {
        return this.previewForm;
    }

    @Override
    protected String initFormTitle() {
        return beanItem.getName();
    }

    @Override
    protected void initRelatedComponents() {
        activityComponent = new ProjectActivityComponent(ProjectTypeConstants.COMPONENT, CurrentProjectVariables.getProjectId());
        dateInfoComp = new DateInfoComp();
        peopleInfoComp = new PeopleInfoComp();

        ProjectView projectView = UIUtils.getRoot(this, ProjectView.class);
        MVerticalLayout detailLayout = new MVerticalLayout().withMargin(new MarginInfo(false, true, true, true));

        if (SiteConfiguration.isCommunityEdition()) {
            detailLayout.with(dateInfoComp, peopleInfoComp);
        } else {
            componentTimeLogComp = new ComponentTimeLogComp();
            detailLayout.with(dateInfoComp, peopleInfoComp, componentTimeLogComp);
        }

        Panel detailPanel = new Panel(UserUIContext.getMessage(GenericI18Enum.OPT_DETAILS), detailLayout);
        UIUtils.makeStackPanel(detailPanel);
        projectView.addComponentToRightBar(detailPanel);
    }

    @Override
    protected void onPreviewItem() {
        activityComponent.loadActivities("" + beanItem.getId());
        dateInfoComp.displayEntryDateTime(beanItem);
        peopleInfoComp.displayEntryPeople(beanItem);

        if (tagViewComponent != null) {
            tagViewComponent.display(ProjectTypeConstants.COMPONENT, beanItem.getId());
        }

        if (componentTimeLogComp != null) {
            componentTimeLogComp.displayTime(beanItem);
        }

        if (StatusI18nEnum.Open.name().equals(beanItem.getStatus())) {
            removeLayoutStyleName(WebThemes.LINK_COMPLETED);
            quickActionStatusBtn.setCaption(UserUIContext.getMessage(GenericI18Enum.BUTTON_CLOSE));
            quickActionStatusBtn.setIcon(VaadinIcons.ARCHIVE);
        } else {
            addLayoutStyleName(WebThemes.LINK_COMPLETED);
            quickActionStatusBtn.setCaption(UserUIContext.getMessage(GenericI18Enum.BUTTON_REOPEN));
            quickActionStatusBtn.setIcon(VaadinIcons.CLIPBOARD);
        }

    }

    @Override
    protected ComponentContainer createExtraControls() {
        if (SiteConfiguration.isCommunityEdition()) {
            return null;
        } else {
            tagViewComponent = new TagViewComponent(CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.COMPONENTS));
            return tagViewComponent;
        }
    }

    @Override
    protected AdvancedPreviewBeanForm<SimpleComponent> initPreviewForm() {
        return new ComponentPreviewForm();
    }

    @Override
    protected HorizontalLayout createButtonControls() {
        ProjectPreviewFormControlsGenerator<SimpleComponent> componentPreviewForm = new ProjectPreviewFormControlsGenerator<>(previewForm);
        HorizontalLayout topPanel = componentPreviewForm.createButtonControls(ProjectRolePermissionCollections.COMPONENTS);
        quickActionStatusBtn = new MButton("", clickEvent -> {
            if (StatusI18nEnum.Closed.name().equals(beanItem.getStatus())) {
                beanItem.setStatus(StatusI18nEnum.Open.name());
                ComponentReadViewImpl.this.removeLayoutStyleName(WebThemes.LINK_COMPLETED);
                quickActionStatusBtn.setCaption(UserUIContext.getMessage(GenericI18Enum.BUTTON_CLOSE));
                quickActionStatusBtn.setIcon(VaadinIcons.ARCHIVE);
            } else {
                beanItem.setStatus(StatusI18nEnum.Closed.name());
                ComponentReadViewImpl.this.addLayoutStyleName(WebThemes.LINK_COMPLETED);
                quickActionStatusBtn.setCaption(UserUIContext.getMessage(GenericI18Enum.BUTTON_REOPEN));
                quickActionStatusBtn.setIcon(VaadinIcons.CLIPBOARD);
            }

            ComponentService service = AppContextUtil.getSpringBean(ComponentService.class);
            service.updateSelectiveWithSession(beanItem, UserUIContext.getUsername());
        }).withStyleName(WebThemes.BUTTON_ACTION);
        componentPreviewForm.insertToControlBlock(quickActionStatusBtn);
        quickActionStatusBtn.setVisible(CurrentProjectVariables.canWrite(ProjectRolePermissionCollections.COMPONENTS));
        return topPanel;
    }

    @Override
    protected ComponentContainer createBottomPanel() {
        return activityComponent;
    }

    @Override
    protected String getType() {
        return ProjectTypeConstants.COMPONENT;
    }

    private static class PeopleInfoComp extends MVerticalLayout {
        private static final long serialVersionUID = 1L;

        void displayEntryPeople(ValuedBean bean) {
            this.removeAllComponents();
            this.withMargin(false);

            Label peopleInfoHeader = new Label(VaadinIcons.USER.getHtml() + " " +
                    UserUIContext.getMessage(ProjectCommonI18nEnum.SUB_INFO_PEOPLE), ContentMode.HTML);
            peopleInfoHeader.setStyleName("info-hdr");
            this.addComponent(peopleInfoHeader);

            GridLayout layout = new GridLayout(2, 2);
            layout.setSpacing(true);
            layout.setWidth("100%");
            layout.setMargin(new MarginInfo(false, false, false, true));
            try {
                Label createdLbl = new Label(UserUIContext.getMessage(ProjectCommonI18nEnum.ITEM_CREATED_PEOPLE));
                createdLbl.setSizeUndefined();
                layout.addComponent(createdLbl, 0, 0);

                String createdUserName = (String) PropertyUtils.getProperty(bean, "createduser");
                String createdUserAvatarId = (String) PropertyUtils.getProperty(bean, "createdUserAvatarId");
                String createdUserDisplayName = (String) PropertyUtils.getProperty(bean, "createdUserFullName");

                ProjectMemberLink createdUserLink = new ProjectMemberLink(createdUserName,
                        createdUserAvatarId, createdUserDisplayName);
                layout.addComponent(createdUserLink, 1, 0);
                layout.setColumnExpandRatio(1, 1.0f);

                Label assigneeLbl = new Label(UserUIContext.getMessage(ProjectCommonI18nEnum.ITEM_ASSIGN_PEOPLE));
                assigneeLbl.setSizeUndefined();
                layout.addComponent(assigneeLbl, 0, 1);
                String assignUserName = (String) PropertyUtils.getProperty(bean, "userlead");
                String assignUserAvatarId = (String) PropertyUtils.getProperty(bean, "userLeadAvatarId");
                String assignUserDisplayName = (String) PropertyUtils.getProperty(bean, "userLeadFullName");

                ProjectMemberLink assignUserLink = new ProjectMemberLink(assignUserName,
                        assignUserAvatarId, assignUserDisplayName);
                layout.addComponent(assignUserLink, 1, 1);
            } catch (Exception e) {
                LOG.error("Can not build user link {} ", BeanUtility.printBeanObj(bean));
            }

            this.addComponent(layout);
        }
    }
}
