package com.databasepreservation.common.shared.client.common.sidebar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.databasepreservation.common.shared.ViewerStructure.ViewerDatabase;
import com.databasepreservation.common.shared.ViewerStructure.ViewerMetadata;
import com.databasepreservation.common.shared.ViewerStructure.ViewerSchema;
import com.databasepreservation.common.shared.ViewerStructure.ViewerTable;
import com.databasepreservation.common.shared.ViewerStructure.ViewerView;
import com.databasepreservation.common.shared.client.tools.FontAwesomeIconManager;
import com.databasepreservation.common.shared.client.tools.HistoryManager;
import com.databasepreservation.common.shared.client.tools.ViewerStringUtils;
import com.databasepreservation.common.shared.client.widgets.wcag.AccessibleFocusPanel;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import config.i18n.client.ClientMessages;

/**
 * @author Bruno Ferreira <bferreira@keep.pt>
 */
public class DatabaseSidebar extends Composite {
  protected static final ClientMessages messages = GWT.create(ClientMessages.class);
  protected static Map<String, DatabaseSidebar> instances = new HashMap<>();

  interface DatabaseSidebarUiBinder extends UiBinder<Widget, DatabaseSidebar> {
  }

  protected static DatabaseSidebarUiBinder uiBinder = GWT.create(DatabaseSidebarUiBinder.class);

  @UiField
  FlowPanel sidebarGroup;

  @UiField
  FlowPanel searchPanel;

  @UiField
  TextBox searchInputBox;

  @UiField
  AccessibleFocusPanel searchInputButton;

  protected ViewerDatabase database;
  protected String databaseUUID;
  protected boolean initialized = false;
  protected static Map<String, SidebarHyperlink> list = new HashMap<>();

  /**
   * Creates a new DatabaseSidebar, rarely hitting the database more than once for
   * each database.
   *
   * @param databaseUUID
   *          the database UUID
   * @return a DatabaseSidebar instance
   */
  public static DatabaseSidebar getInstance(String databaseUUID) {

    if (databaseUUID == null) {
      return getEmptyInstance();
    }

    DatabaseSidebar instance = instances.get(databaseUUID);
    if (instance == null || instance.database == null
      || !ViewerDatabase.Status.AVAILABLE.equals(instance.database.getStatus())) {
      instance = new DatabaseSidebar(databaseUUID);
      instances.put(databaseUUID, instance);
    } else {
      // workaround because the same DatabaseSidebar can not belong to multiple
      // widgets
      return new DatabaseSidebar(instance);
    }
    return instance;
  }

  /**
   * Creates a new DatabaseSidebar, rarely hitting the database more than once for
   * each database.
   *
   * @param database
   *          the database
   * @return a DatabaseSidebar instance
   */
  public static DatabaseSidebar getInstance(ViewerDatabase database) {
    if (database == null) {
      return getEmptyInstance();
    }

    DatabaseSidebar instance = instances.get(database.getUUID());
    if (instance == null || instance.database == null
      || !ViewerDatabase.Status.AVAILABLE.equals(instance.database.getStatus())) {
      instance = new DatabaseSidebar(database);
      instances.put(database.getUUID(), instance);
    } else {
      // workaround because the same DatabaseSidebar can not belong to multiple
      // widgets
      return new DatabaseSidebar(instance);
    }
    return instance;
  }

  /**
   * Creates a new (dummy) DatabaseSidebar that is not visible. This method exists
   * so that pages can opt for not using a sidebar at all.
   *
   * @return a new invisible DatabaseSidebar
   */
  public static DatabaseSidebar getEmptyInstance() {
    return new DatabaseSidebar();
  }

  /**
   * Clone constructor, because the same DatabaseSidebar can not be child in more
   * than one widget
   *
   * @param other
   *          the DatabaseSidebar used in another widget
   */
  private DatabaseSidebar(DatabaseSidebar other) {
    initialized = other.initialized;
    initWidget(uiBinder.createAndBindUi(this));
    searchInputBox.setText(other.searchInputBox.getText());
    init(other.database);
  }

  /**
   * Use DatabaseSidebar.getInstance to obtain an instance
   */
  private DatabaseSidebar(ViewerDatabase database) {
    initWidget(uiBinder.createAndBindUi(this));
    init(database);
  }

  /**
   * Empty constructor, for pages that do not have a sidebar
   */
  private DatabaseSidebar() {
    initWidget(uiBinder.createAndBindUi(this));
    this.setVisible(false);
  }

  /**
   * Use DatabaseSidebar.getInstance to obtain an instance
   */
  private DatabaseSidebar(String databaseUUID) {
    this();
    this.databaseUUID = databaseUUID;
  }

  public void init() {
    // database metadata
    final ViewerMetadata metadata = database.getMetadata();

    SidebarHyperlink informationLink = new SidebarHyperlink(FontAwesomeIconManager
      .getTagSafeHtml(FontAwesomeIconManager.DATABASE_INFORMATION, messages.menusidebar_information()),
      HistoryManager.linkToDatabase(database.getUUID()));
    informationLink.setH5().setIndent0();
    list.put(HistoryManager.ROUTE_DATABASE, informationLink);
    sidebarGroup.add(informationLink);

    SidebarHyperlink searchLink = new SidebarHyperlink(FontAwesomeIconManager
      .getTagSafeHtml(FontAwesomeIconManager.DATABASE_SEARCH, messages.menusidebar_searchAllRecords()),
      HistoryManager.linkToDatabaseSearch(database.getUUID()));
    searchLink.setH5().setIndent0();
    list.put(HistoryManager.ROUTE_DATABASE_SEARCH, searchLink);
    sidebarGroup.add(searchLink);

    SidebarHyperlink savedSearchesLink = new SidebarHyperlink(
      FontAwesomeIconManager.getTagSafeHtml(FontAwesomeIconManager.SAVED_SEARCH, messages.menusidebar_savedSearches()),
      HistoryManager.linkToSavedSearches(database.getUUID()));
    savedSearchesLink.setH5().setIndent0();
    list.put(HistoryManager.ROUTE_SAVED_SEARCHES, savedSearchesLink);
    sidebarGroup.add(savedSearchesLink);

    /* Schemas */
    SidebarItem schemasHeader = createSidebarSubItemHeaderSafeHMTL("Tables", FontAwesomeIconManager.LIST);
    FlowPanel schemaItems = new FlowPanel();

    final int totalSchemas = metadata.getSchemas().size();

    String iconTag = FontAwesomeIconManager.getTagWithStyleName(FontAwesomeIconManager.SCHEMA_TABLE_SEPARATOR, "fa-sm");

    for (final ViewerSchema schema : metadata.getSchemas()) {
      schema.setViewsSchemaUUID();

      for (ViewerTable table : schema.getTables()) {
        if (!table.isCustomView() && !table.isMaterializedView()) {
          schemaItems.add(createTableItem(schema, table, totalSchemas, iconTag));
        }
      }

      for (ViewerView view : schema.getViews()) {
        schemaItems.add(createViewItem(schema, view, totalSchemas, iconTag));
      }
    }

      createSubItem(schemasHeader, schemaItems, true);

    /* Technical Information */
    SidebarItem technicalHeader = createSidebarSubItemHeaderSafeHMTL("Technical Information",
      FontAwesomeIconManager.TECHNICAL);
    FlowPanel technicalItems = new FlowPanel();

    SidebarHyperlink routinesLink = new SidebarHyperlink(
      FontAwesomeIconManager.getTagSafeHtml(FontAwesomeIconManager.SCHEMA_ROUTINES, messages.menusidebar_routines()),
      HistoryManager.linkToSchemaRoutines(database.getUUID()));
    routinesLink.setH6().setIndent1();
    list.put(HistoryManager.ROUTE_SCHEMA_ROUTINES, routinesLink);
    sidebarGroup.add(routinesLink);
    technicalItems.add(routinesLink);

    SidebarHyperlink usersLink = new SidebarHyperlink(
      FontAwesomeIconManager.getTagSafeHtml(FontAwesomeIconManager.DATABASE_USERS, messages.menusidebar_usersRoles()),
      HistoryManager.linkToDatabaseUsers(database.getUUID()));
    usersLink.setH6().setIndent1();
    list.put(HistoryManager.ROUTE_DATABASE_USERS, usersLink);
    sidebarGroup.add(usersLink);
    technicalItems.add(usersLink);

    SidebarHyperlink reportLink = new SidebarHyperlink(
      FontAwesomeIconManager.getTagSafeHtml(FontAwesomeIconManager.DATABASE_REPORT, messages.titleReport(), true),
      HistoryManager.linkToDatabaseReport(database.getUUID()));
    reportLink.setH6().setIndent1();
    list.put(HistoryManager.ROUTE_DATABASE_REPORT, reportLink);
    sidebarGroup.add(reportLink);
    technicalItems.add(reportLink);

    createSubItem(technicalHeader, technicalItems, true);

    searchInit();
    setVisible(true);
  }

  private SidebarHyperlink createTableItem(final ViewerSchema schema, final ViewerTable table, final int totalSchemas,
    final String iconTag) {
      SafeHtml html;
      if (totalSchemas == 1) {
        html = FontAwesomeIconManager.getTagSafeHtml(FontAwesomeIconManager.TABLE, table.getName());
      } else {
        html = FontAwesomeIconManager.getTagSafeHtml(FontAwesomeIconManager.TABLE,
            schema.getName() + " " + iconTag + " " + table.getName());
      }
      SidebarHyperlink tableLink = new SidebarHyperlink(html,
          HistoryManager.linkToTable(database.getUUID(), table.getUUID()));
      tableLink.setH6().setIndent2();
      list.put(table.getUUID(), tableLink);
      sidebarGroup.add(tableLink);

      return tableLink;
  }

  private SidebarHyperlink createViewItem(final ViewerSchema schema, final ViewerView view, final int totalSchemas,
    final String iconTag) {
    SafeHtml html;
    SidebarHyperlink viewLink;
    final ViewerTable materializedTable = schema.getMaterializedTable(view.getName());
    if (materializedTable != null) {
      if (totalSchemas == 1) {
        html = FontAwesomeIconManager.getStackedIconSafeHtml(FontAwesomeIconManager.SCHEMA_VIEWS,
            FontAwesomeIconManager.TABLE, materializedTable.getNameWithoutPrefix());
      } else {
        html = FontAwesomeIconManager.getStackedIconSafeHtml(FontAwesomeIconManager.SCHEMA_VIEWS,
            FontAwesomeIconManager.TABLE, schema.getName() + " " + iconTag + " " + materializedTable.getNameWithoutPrefix());
      }
      viewLink = new SidebarHyperlink(html,
        HistoryManager.linkToTable(database.getUUID(), materializedTable.getUUID())).setTooltip("Materialized View");
      list.put(materializedTable.getUUID(), viewLink);
    } else if (schema.getCustomViewTable(view.getName()) != null) {
      final ViewerTable customViewTable = schema.getCustomViewTable(view.getName());
      if (totalSchemas == 1) {
        html = FontAwesomeIconManager.getStackedIconSafeHtml(FontAwesomeIconManager.SCHEMA_VIEWS,
          FontAwesomeIconManager.COG, customViewTable.getNameWithoutPrefix());
      } else {
        html = FontAwesomeIconManager.getStackedIconSafeHtml(FontAwesomeIconManager.SCHEMA_VIEWS,
          FontAwesomeIconManager.COG, schema.getName() + " " + iconTag + " " + customViewTable.getNameWithoutPrefix());
      }
      viewLink = new SidebarHyperlink(html, HistoryManager.linkToTable(database.getUUID(), customViewTable.getUUID())).setTooltip("Custom View");
      list.put(customViewTable.getUUID(), viewLink);
    } else {
      if (totalSchemas == 1) {
        html = FontAwesomeIconManager.getTagSafeHtml(FontAwesomeIconManager.SCHEMA_VIEWS, view.getName());
      } else {
        html = FontAwesomeIconManager.getTagSafeHtml(FontAwesomeIconManager.SCHEMA_VIEWS,
          schema.getName() + " " + iconTag + " " + view.getName());
      }
      viewLink = new SidebarHyperlink(html, HistoryManager.linkToView(database.getUUID(), view.getUUID()));
      list.put(view.getUUID(), viewLink);
    }

    viewLink.setH6().setIndent2();

    sidebarGroup.add(viewLink);

    return viewLink;
  }

  public boolean isInitialized() {
    return initialized;
  }

  public void init(ViewerDatabase db) {
    GWT.log("init with db: " + db + "; status: " + db.getStatus().toString());
    if (ViewerDatabase.Status.AVAILABLE.equals(db.getStatus())) {
      if (db != null && (databaseUUID == null || databaseUUID.equals(db.getUUID()))) {
        initialized = true;
        database = db;
        databaseUUID = db.getUUID();
        init();
      }
    }
  }

  private SidebarItem createSidebarSubItemHeaderSafeHMTL(String headerText, String headerIcon) {
    return new SidebarItem(FontAwesomeIconManager.getTagSafeHtml(headerIcon, headerText)).setH5().setIndent0();
  }

  private void createSubItem(SidebarItem header, FlowPanel content, boolean collapsed) {
    DisclosurePanel panel = new DisclosurePanel();
    panel.setOpen(!collapsed);
    panel.setAnimationEnabled(true);
    panel.setHeader(header);
    panel.setContent(content);
    panel.getElement().addClassName("sidebar-collapse");
    sidebarGroup.add(panel);
  }

  public void select(String value) {
    for (Map.Entry<String, SidebarHyperlink> entry : list.entrySet()) {
      if (entry.getKey().equals(value)) {
        list.get(value).setSelected(true);
      } else {
        list.get(entry.getKey()).setSelected(false);
      }
    }
  }

  private void searchInit() {
    searchInputBox.getElement().setPropertyString("placeholder", messages.menusidebar_filterSidebar());

    searchInputBox.addChangeHandler(event -> doSearch());

    searchInputBox.addKeyUpHandler(event -> doSearch());

    searchInputButton.addClickHandler(event -> doSearch());
  }

  private void doSearch() {
    String searchValue = searchInputBox.getValue();

    if (ViewerStringUtils.isBlank(searchValue)) {
      showAll();
    } else {
      showMatching(searchValue);
    }
  }

  private void showAll() {
    // show all
    for (Widget widget : sidebarGroup) {
      widget.setVisible(true);

      if (widget instanceof DisclosurePanel) {
        DisclosurePanel disclosurePanel = (DisclosurePanel) widget;
        FlowPanel fp = (FlowPanel) disclosurePanel.getContent();
        for (Widget value : fp) {
          SidebarItem sb = (SidebarItem) value;
          sb.setVisible(true);
        }
      }
    }
  }

  private void showMatching(final String searchValue) {
    // show matching and their parents
    Set<DisclosurePanel> disclosurePanelsThatShouldBeVisible = new HashSet<>();

    for (Widget widget : sidebarGroup) {
      if (widget instanceof DisclosurePanel) {
        DisclosurePanel disclosurePanel = (DisclosurePanel) widget;
        disclosurePanel.setOpen(true);
        FlowPanel fp = (FlowPanel) disclosurePanel.getContent();

        for (Widget value : fp) {
          SidebarItem sb = (SidebarItem) value;
          if (sb.getText().toLowerCase().contains(searchValue.toLowerCase())) {
            sb.setVisible(true);
            disclosurePanelsThatShouldBeVisible.add(disclosurePanel);
          } else {
            sb.setVisible(false);
            disclosurePanel.setVisible(false);
          }
        }
      } else {
        widget.setVisible(true);
      }
    }

    for (DisclosurePanel disclosurePanel : disclosurePanelsThatShouldBeVisible) {
      disclosurePanel.setVisible(true);
    }
  }
}
