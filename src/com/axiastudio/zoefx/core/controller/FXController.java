package com.axiastudio.zoefx.core.controller;

import com.axiastudio.zoefx.core.beans.BeanAccess;
import com.axiastudio.zoefx.core.beans.BeanClassAccess;
import com.axiastudio.zoefx.core.beans.property.ItemObjectProperty;
import com.axiastudio.zoefx.core.beans.property.ZoeFXProperty;
import com.axiastudio.zoefx.core.db.TimeMachine;
import com.axiastudio.zoefx.core.events.DataSetEvent;
import com.axiastudio.zoefx.core.events.DataSetEventListener;
import com.axiastudio.zoefx.core.db.DataSet;
import com.axiastudio.zoefx.core.view.*;
import com.axiastudio.zoefx.core.console.ConsoleController;
import com.axiastudio.zoefx.core.view.search.SearchController;
import javafx.beans.*;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: tiziano
 * Date: 20/03/14
 * Time: 23:04
 */
public class FXController extends BaseController implements DataSetEventListener {

    private Scene scene;
    private DataSet dataset = null;
    private ZSceneMode mode;
    private Behavior behavior = null;
    private TimeMachine timeMachine = null;
    private Map<String, Property> fxProperties = new HashMap<>();
    private Map<String,TableView> tableViews;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void setScene(Scene scene){
        this.scene = scene;
    }

    public void bindDataSet(DataSet dataset){
        this.dataset = dataset;
        Model model = dataset.newModel();
        scanFXProperties();
        initializeChoices();
        initializeColumns();
        // first show
        setModel(model);
        timeMachine.createSnapshot(fxProperties.values());
    }


    private void initializeColumns(){
        Model model = dataset.getCurrentModel();
        Parent root = this.scene.getRoot();
        Pane container = (Pane) root;
        List<Node> nodes = findNodes(container, new ArrayList<Node>());
        tableViews = new HashMap<>();
        for( Node node: nodes ){
            if( node instanceof TableView){
                TableView tableView = (TableView) node;
                tableViews.put(node.getId(), tableView);
                ObservableList<TableColumn> columns = tableView.getColumns();
                for( TableColumn column: columns ){
                    String name = node.getId();
                    //String columnId = column.getId();
                    String columnId = column.getText().toLowerCase(); // XXX: RT-36633 JavaXFX issue
                    // https://javafx-jira.kenai.com/browse/RT-36633
                    String lookup=null;
                    if( behavior != null ) {
                        lookup = behavior.getProperties().getProperty(columnId + ".lookup");
                    }
                    if( lookup != null ) {
                        Callback callback = model.getCallback(name, columnId, lookup);
                        column.setCellValueFactory(callback);
                    } else {
                        Callback callback = model.getCallback(name, columnId);
                        column.setCellValueFactory(callback);
                    }
                    // custom date order
                    BeanClassAccess beanClassAccess = new BeanClassAccess(model.getEntityClass(), columnId);
                    if( beanClassAccess.getReturnType() != null && Date.class.isAssignableFrom(beanClassAccess.getReturnType()) ) {
                        column.setComparator(Comparator.nullsFirst(Comparators.DateComparator));
                    }
                    //tableView.getItems().addListener(listChangeListener);
                }
            }
        }
    }

    private void initializeChoices(){
        Model model = dataset.getCurrentModel();
        Parent root = this.scene.getRoot();
        Pane container = (Pane) root;
        List<Node> nodes = findNodes(container, new ArrayList<Node>());
        for( Node node: nodes ){
            if( node instanceof ChoiceBox){
                String name = node.getId();
                Property property = model.getProperty(name, Object.class);
                List superset = ((ItemObjectProperty) property).getSuperset();
                ObservableList choices = FXCollections.observableArrayList(superset);
                ((ChoiceBox) node).setItems(choices);
            }
        }
    }

    private void unsetModel() {
        Model model = dataset.getCurrentModel();
        for( String name: fxProperties.keySet() ){
            Property fxProperty = fxProperties.get(name);
            ZoeFXProperty zoeFXProperty = model.getProperty(name);
            if( fxProperty != null && zoeFXProperty != null ) {
                Bindings.unbindBidirectional(fxProperty, zoeFXProperty);
                fxProperty.removeListener(invalidationListener);
            }
        }
    }

    private void setModel() {
        setModel(dataset.newModel());
    }

    private void setModel(Model model) {
        for( String name: fxProperties.keySet() ){
            Property fxProperty = fxProperties.get(name);
            ZoeFXProperty zoeFXProperty = model.getProperty(name);
            if( zoeFXProperty == null ){
                Node node = scene.lookup("#"+name);
                if( node instanceof TextField ){
                    zoeFXProperty = model.getProperty(name, String.class);
                } else if( node instanceof TextArea ){
                    zoeFXProperty = model.getProperty(name, String.class);
                } else if( node instanceof CheckBox ){
                    zoeFXProperty = model.getProperty(name, Boolean.class);
                } else if( node instanceof ChoiceBox ){
                    zoeFXProperty = model.getProperty(name, Object.class);
                } else if( node instanceof DatePicker ){
                    zoeFXProperty = model.getProperty(name, Date.class);
                } else if( node instanceof TableView ){
                    zoeFXProperty = model.getProperty(name, Collection.class);
                }
            }
            if( fxProperty != null && zoeFXProperty != null ) {
                Bindings.bindBidirectional(fxProperty, zoeFXProperty);
                fxProperty.addListener(invalidationListener);
            }
        }
        //updateCache();
    }

    private void scanFXProperties(){
        Parent root = scene.getRoot();
        Pane container = (Pane) root;
        List<Node> nodes = findNodes(container, new ArrayList<>());
        for( Node node: nodes ){
            String name = node.getId();
            Property property = null;
            if( node instanceof TextField ){
                property = ((TextField) node).textProperty();
            } else if( node instanceof TextArea ){
                property = ((TextArea) node).textProperty();
            } else if( node instanceof CheckBox ){
                property = ((CheckBox) node).selectedProperty();
            } else if( node instanceof ChoiceBox ){
                property = ((ChoiceBox) node).valueProperty();
            } else if( node instanceof DatePicker ){
                property = ((DatePicker) node).valueProperty();
            } else if( node instanceof TableView ){
                TableView tableView = (TableView) node;
                tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                tableView.setContextMenu(createContextMenu(tableView));
                property = tableView.itemsProperty();
            }
            if( property != null ){
                fxProperties.put(name, property);
            }
        }

        /*
        Validator validator = Validators.getValidator(model.getEntityClass(), name);
                if( validator != null ) {
                    fxProperty.addListener(new TextFieldListener(validator));
                }
         */
    }


    private ContextMenu createContextMenu(TableView tableView){
        ContextMenu contextMenu = new ContextMenu();

        MenuItem infoItem = new MenuItem("Information");
        infoItem.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/com/axiastudio/zoefx/core/resources/info.png"))));
        infoItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                ObservableList selectedItems = tableView.getSelectionModel().getSelectedItems();
                if( selectedItems.size()==0 ) {
                    return;
                }
                List<Object> newStore = new ArrayList<>();
                for( int i=0; i<selectedItems.size(); i++ ) {
                    newStore.add(selectedItems.get(i));
                }
                ZScene newScene = SceneBuilders.queryZScene(newStore, ZSceneMode.DIALOG);
                if( newScene != null ) {
                    Stage newStage = new Stage();
                    newStage.setScene(newScene.getScene());
                    newStage.show();
                }
            }
        });
        MenuItem openItem = new MenuItem("Open");
        openItem.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/com/axiastudio/zoefx/core/resources/open.png"))));
                openItem.setOnAction(new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent e) {
                        ObservableList selectedItems = tableView.getSelectionModel().getSelectedItems();
                        if (selectedItems.size() == 0) {
                            return;
                        }
                        List<Object> newStore = new ArrayList<>();
                        String referenceProperty = tableView.getId() + ".reference";
                        String reference = behavior.getProperties().getProperty(referenceProperty, null);
                        for (int i = 0; i < selectedItems.size(); i++) {
                            Object item = selectedItems.get(i);
                            if (reference != null) {
                                BeanAccess<Object> ba = new BeanAccess<>(item, reference);
                                newStore.add(ba.getValue());
                            } else {
                                newStore.add(item);
                            }
                        }
                        ZScene newScene = SceneBuilders.queryZScene(newStore, ZSceneMode.WINDOW);
                        if (newScene != null) {
                            Stage newStage = new Stage();
                            newStage.setScene(newScene.getScene());
                            newStage.show();
                        }
                    }
                });
        MenuItem addItem = new MenuItem("Add");
        addItem.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/com/axiastudio/zoefx/core/resources/add.png"))));
        addItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                final String collectionName = tableView.getId();
                String referenceProperty = collectionName + ".reference";
                String searchcolumnsProperty = collectionName + ".searchcolumns";
                String referenceName = behavior.getProperties().getProperty(referenceProperty, null);
                String searchcolumns = behavior.getProperties().getProperty(searchcolumnsProperty, "caption"); // XXX: default caption?
                if (referenceName != null) {
                    Class classToSearch = null;
                    try {
                        Class parentEntityClass = dataset.getCurrentModel().getEntityClass();
                        Class<?> collectionGenericReturnType = (new BeanClassAccess(parentEntityClass, collectionName)).getGenericReturnType();
                        Class<?> referenceReturnType = (new BeanClassAccess(collectionGenericReturnType, referenceName)).getReturnType();
                        String className = referenceReturnType.getName();
                        classToSearch = Class.forName(className);
                        Callback callback = new Callback<List, Boolean>() {
                            @Override
                            public Boolean call(List items) {
                                for( Object item: items ){
                                    Object entity = dataset.create(collectionName);
                                    BeanAccess<Object> ba = new BeanAccess<>(entity, referenceName);
                                    ba.setValue(item);
                                    refresh();
                                }
                                return true;
                            }
                        };
                        Stage stage = searchStage(classToSearch, searchcolumns, callback);
                        stage.show();
                    } catch (ClassNotFoundException e1) {
                        e1.printStackTrace();
                    }
                } else {
                    Object entity = dataset.create(collectionName);
                    List<Object> newStore = new ArrayList<>();
                    newStore.add(entity);
                    ZScene newScene = SceneBuilders.queryZScene(newStore, ZSceneMode.DIALOG);
                    if (newScene != null) {
                        Stage newStage = new Stage();
                        newStage.setScene(newScene.getScene());
                        newStage.show();
                    }
                }
                //refreshModel();
                initializeColumns();
                dataset.getDirty();
            }
        });
        MenuItem delItem = new MenuItem("Delete");
        delItem.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/com/axiastudio/zoefx/core/resources/delete.png"))));
        delItem.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent e) {
                System.out.println("Delete!");
            }
        });

        contextMenu.getItems().addAll(infoItem, openItem, addItem, delItem);
        return contextMenu;
    }



    private List<Node> findNodes( Pane container, List<Node> nodes ){
        for( Node node: container.getChildren() ){
            if( node instanceof Pane ){
                nodes = findNodes((Pane) node, nodes);
            } else if( node instanceof TabPane ){
                for( Tab tab: ((TabPane) node).getTabs() ) {
                    nodes = findNodes((Pane) tab.getContent(), nodes);
                }
            }
            else if( node.getId() != null && node.getId() != "" ){
                nodes.add(node);
            }
        }
        return nodes;
    }


    public DataSet getDataset() {
        return dataset;
    }

    private FXController self(){
        return this;
    }

    public ZSceneMode getMode() {
        return mode;
    }

    public void setMode(ZSceneMode mode) {
        this.mode = mode;
    }

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    public void setTimeMachine(TimeMachine timeMachine) {
        this.timeMachine = timeMachine;
    }

    public void refresh(){
        unsetModel();
        Model model = dataset.newModel();
        setModel(model);
        timeMachine.resetAndcreateSnapshot(fxProperties.values());
        for( TableView tableView: tableViews.values() ){
            // XXX: workaround for https://javafx-jira.kenai.com/browse/RT-22599
            ObservableList items = tableView.getItems();
            tableView.setItems(null);
            tableView.layout();
            tableView.setItems(items);
        }
    }

    private Stage searchStage(Class classToSearch, String searchcolumns, Callback callback) {
        URL url = getClass().getResource("/com/axiastudio/zoefx/core/view/search/search.fxml");
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(url);
        loader.setBuilderFactory(new JavaFXBuilderFactory());
        Parent root = null;
        try {
            root = loader.load(url.openStream());
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        SearchController controller = loader.getController();
        controller.setEntityClass(classToSearch);
        List<String> columns = new ArrayList<>();
        if( searchcolumns != null ){
            String[] split = searchcolumns.split(",");
            for( int i=0; i<split.length; i++ ){
                columns.add(split[i]);
            }
        }
        controller.setColumns(columns);
        controller.setCallback(callback);
        //controller.setParentDataSet(dataset);

        Stage stage = new Stage();
        stage.setTitle("Search");
        stage.setScene(new Scene(root, 450, 450));
        return stage;
    }

    /*
     *  Navigation Bar
     */

    public EventHandler<ActionEvent> handlerGoFirst = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            unsetModel();
            dataset.goFirst();
            setModel(dataset.newModel());
            timeMachine.resetAndcreateSnapshot(fxProperties.values());
        }
    };
    public EventHandler<ActionEvent> handlerGoPrevious = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            unsetModel();
            dataset.goPrevious();
            setModel(dataset.newModel());
            timeMachine.resetAndcreateSnapshot(fxProperties.values());
        }
    };
    public EventHandler<ActionEvent> handlerGoNext = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            unsetModel();
            dataset.goNext();
            setModel(dataset.newModel());
            timeMachine.resetAndcreateSnapshot(fxProperties.values());
        }
    };
    public EventHandler<ActionEvent> handlerGoLast = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            unsetModel();
            dataset.goLast();
            setModel(dataset.newModel());
            timeMachine.resetAndcreateSnapshot(fxProperties.values());
        }
    };
    public EventHandler<ActionEvent> handlerSave = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            dataset.commit();
            timeMachine.resetAndcreateSnapshot(fxProperties.values());
        }
    };
    public EventHandler<ActionEvent> handlerConfirm = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            ((Stage) scene.getWindow()).close();
            // TODO: get the parent dirty
        }
    };
    public EventHandler<ActionEvent> handlerCancel = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e)
        {
            timeMachine.rollback();
            dataset.revert();
            timeMachine.resetAndcreateSnapshot(fxProperties.values());
        }
    };
    public EventHandler<ActionEvent> handlerAdd = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            dataset.create();
            unsetModel();
            setModel(dataset.newModel());
        }
    };
    public EventHandler<ActionEvent> handlerSearch = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            Class classToSearch = dataset.getCurrentModel().getEntityClass();
            String searchcolumns = behavior.getProperties().getProperty("searchcolumns");
            Callback callback = new Callback<List, Boolean>() {
                @Override
                public Boolean call(List items) {
                    List store = new ArrayList();
                    for( Object item: items ){
                        store.add(item);
                    }
                    dataset.setStore(store);
                    return true;
                }
            };
            Stage stage = searchStage(classToSearch, searchcolumns, callback);
            stage.show();
        }

    };
    public EventHandler<ActionEvent> handlerDelete = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            dataset.delete();
        }
    };
    public EventHandler<ActionEvent> handlerRefresh = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            refresh();
        }
    };
    public EventHandler<ActionEvent> handlerConsole = new EventHandler<ActionEvent>() {
        @Override
        public void handle(ActionEvent e) {
            URL url = getClass().getResource("/com/axiastudio/zoefx/core/console/console.fxml");
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(url);
            loader.setBuilderFactory(new JavaFXBuilderFactory());
            Parent root = null;
            try {
                root = loader.load(url.openStream());
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            ConsoleController console = loader.getController();
            console.setController(self());

            Stage stage = new Stage();
            stage.setTitle("Zoe FX Script Console");
            stage.setScene(new Scene(root, 450, 450));
            stage.show();
        }
    };


    /*
     *  Listeners
     */

    public InvalidationListener invalidationListener = new InvalidationListener() {
        @Override
        public void invalidated(Observable observable) {
            dataset.getDirty();
        }
    };


    @Override
    public void dataSetEventHandler(DataSetEvent event) {
        System.out.println(event.getEventType());
        Logger.getLogger(this.getClass().getName()).log(Level.FINE, "{0} event handled", event.getEventType().getName());
        if( event.getEventType().equals(DataSetEvent.STORE_CHANGED) ){
            unsetModel();
            setModel();
        } else if( event.getEventType().equals(DataSetEvent.ROWS_CREATED) ){
            refresh();
        }
    }
}