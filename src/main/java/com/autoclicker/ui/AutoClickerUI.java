package com.autoclicker.ui;

import com.autoclicker.config.AutoClickerConfig;
import com.autoclicker.config.AutoClickerConfig.ClickType;
import com.autoclicker.config.AutoClickerConfig.MouseButton;
import com.autoclicker.config.ConfigRepository;
import com.autoclicker.engine.AutoClickerEngine;
import com.autoclicker.engine.ExecutionEvent;
import com.autoclicker.engine.SessionMetrics;
import com.autoclicker.listener.HotkeyListener;
import com.autoclicker.macro.ClickAction;
import com.autoclicker.macro.MacroSequence;
import com.autoclicker.presenter.AutoClickerPresenter;
import com.autoclicker.presenter.ExecutionState;
import com.autoclicker.presenter.IAutoClickerView;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

@SuppressWarnings("serial")
public final class AutoClickerUI extends JFrame implements IAutoClickerView {
    private final AutoClickerConfig config = new AutoClickerConfig();
    private final AutoClickerEngine engine;
    private final ConfigRepository configRepository = new ConfigRepository();
    private final AutoClickerPresenter presenter;
    private final HotkeyListener hotkeyListener;

    private JSpinner spinnerInterval;
    private JComboBox<MouseButton> comboMouseButton;
    private JComboBox<ClickType> comboClickType;
    private JTextField textFieldHotkey;
    private JSpinner spinnerRepeat;
    private JSpinner spinnerStartDelay;
    private JCheckBox checkInfinite;
    private JCheckBox checkFixedPos;
    private JSpinner spinnerFixX;
    private JSpinner spinnerFixY;
    private JButton btnCapture;
    private JComboBox<String> comboProfiles;
    private JButton btnProfileSave;
    private JButton btnProfileLoad;
    private JButton btnProfileDelete;
    private JButton btnChangeHotkey;
    private JCheckBox checkMacroMode;
    private JTable macroTable;
    private MacroTableModel macroTableModel;
    private final List<JButton> macroButtons = new ArrayList<>();

    private JLabel labelStatus;
    private JLabel labelActionCount;
    private JLabel labelElapsed;
    private JLabel labelRate;
    private JLabel labelHotkeyHint;
    private JProgressBar progressBar;
    private JButton btnToggle;

    private boolean capturingPosition;
    private boolean capturingHotkey;
    private int backgroundOperations;
    private Timer captureTimer;
    private JDialog captureDialog;
    private ExecutionState executionState = ExecutionState.STOPPED;

    private static final Color GREEN = new Color(76, 175, 80);
    private static final Color RED = new Color(244, 67, 54);
    private static final Color BLUE = new Color(33, 150, 243);
    private static final Color AMBER = new Color(255, 193, 7);
    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font LABEL_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public AutoClickerUI() {
        applyLookAndFeel();
        engine = new AutoClickerEngine(config);
        presenter = new AutoClickerPresenter(config, engine, this);
        hotkeyListener = new HotkeyListener(presenter);
        initializeUI();
        presenter.setStartValidator(this::commitEditorsToConfig);
        onExecutionStateChanged(ExecutionState.STOPPED, false);
        loadProfileNames();
        try {
            hotkeyListener.register();
        } catch (RuntimeException e) {
            SwingUtilities.invokeLater(() -> onExecutionError("O atalho global não pôde ser ativado.", e));
        }
    }

    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
        } catch (Exception e) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
        }
    }

    private void initializeUI() {
        setTitle("AutoClicker Pro");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { shutdown(); }
        });

        JPanel settings = new JPanel();
        settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
        settings.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        settings.add(createHeader());
        settings.add(Box.createVerticalStrut(10));
        settings.add(createProfileSection());
        settings.add(Box.createVerticalStrut(10));
        settings.add(createIntervalSection());
        settings.add(Box.createVerticalStrut(10));
        settings.add(createMouseSection());
        settings.add(Box.createVerticalStrut(10));
        settings.add(createRepeatSection());
        settings.add(Box.createVerticalStrut(10));
        settings.add(createHotkeySection());
        settings.add(Box.createVerticalStrut(10));
        settings.add(createMacroSection());

        JScrollPane scroll = new JScrollPane(settings, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel frame = new JPanel(new BorderLayout());
        frame.add(scroll, BorderLayout.CENTER);
        frame.add(createStatusAndControlFooter(), BorderLayout.SOUTH);
        setContentPane(frame);
        pack();

        Rectangle usable = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        Dimension preferred = getPreferredSize();
        int width = Math.min(Math.max(480, preferred.width), usable.width);
        int height = Math.min(Math.max(560, preferred.height), usable.height);
        setSize(width, height);
        setMinimumSize(new Dimension(Math.min(480, usable.width), Math.min(500, usable.height)));
        setLocationRelativeTo(null);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel title = new JLabel("AutoClicker Pro");
        title.setFont(TITLE_FONT);
        title.setForeground(BLUE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel subtitle = new JLabel("Automação de cliques para Windows");
        subtitle.setFont(LABEL_FONT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(subtitle);
        return panel;
    }

    private JPanel createProfileSection() {
        JPanel card = card("Perfis");
        JPanel content = new JPanel(new BorderLayout(6, 6));
        comboProfiles = new JComboBox<>();
        comboProfiles.setEditable(true);
        content.add(comboProfiles, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 3, 6, 0));
        btnProfileSave = new JButton("Salvar");
        btnProfileLoad = new JButton("Carregar");
        btnProfileDelete = new JButton("Excluir");
        btnProfileSave.addActionListener(event -> saveProfile());
        btnProfileLoad.addActionListener(event -> loadProfile());
        btnProfileDelete.addActionListener(event -> deleteProfile());
        actions.add(btnProfileSave);
        actions.add(btnProfileLoad);
        actions.add(btnProfileDelete);
        content.add(actions, BorderLayout.SOUTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createIntervalSection() {
        JPanel card = card("Intervalo");
        JPanel content = new FlowLayoutPanel();
        content.add(new JLabel("Pausa após cada ação de clique:"));
        spinnerInterval = new JSpinner(new SpinnerNumberModel(0.2, 0.001, 60.0, 0.01));
        spinnerInterval.setEditor(new JSpinner.NumberEditor(spinnerInterval, "0.###"));
        spinnerInterval.setPreferredSize(new Dimension(100, 32));
        content.add(spinnerInterval);
        content.add(new JLabel("segundos"));
        JLabel rate = new JLabel("(≈ 5 ações/s)");
        spinnerInterval.addChangeListener(event -> {
            double value = ((Number) spinnerInterval.getValue()).doubleValue();
            rate.setText(String.format("(≈ %.1f ações/s)", 1.0 / value));
        });
        content.add(rate);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMouseSection() {
        JPanel card = card("Mouse");
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JPanel choices = new JPanel(new GridLayout(2, 2, 8, 8));
        choices.add(new JLabel("Botão:"));
        comboMouseButton = new JComboBox<>(MouseButton.values());
        comboMouseButton.setRenderer(enumRenderer());
        choices.add(comboMouseButton);
        choices.add(new JLabel("Tipo:"));
        comboClickType = new JComboBox<>(ClickType.values());
        comboClickType.setRenderer(enumRenderer());
        choices.add(comboClickType);

        JPanel position = new FlowLayoutPanel();
        checkFixedPos = new JCheckBox("Usar posição fixa");
        spinnerFixX = coordinateSpinner();
        spinnerFixY = coordinateSpinner();
        btnCapture = new JButton("Capturar em 3s");
        checkFixedPos.addActionListener(event -> updateControlAvailability());
        btnCapture.addActionListener(event -> beginPositionCapture(point -> {
            spinnerFixX.setValue(point.x);
            spinnerFixY.setValue(point.y);
        }));
        position.add(checkFixedPos);
        position.add(new JLabel("X:")); position.add(spinnerFixX);
        position.add(new JLabel("Y:")); position.add(spinnerFixY);
        position.add(btnCapture);
        content.add(choices);
        content.add(position);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createRepeatSection() {
        JPanel card = card("Repetição e início");
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        checkInfinite = new JCheckBox("Executar até parar manualmente", true);
        checkInfinite.addActionListener(event -> updateControlAvailability());
        JPanel limit = new FlowLayoutPanel();
        limit.add(new JLabel("Limite de ações/ciclos:"));
        spinnerRepeat = new JSpinner(new SpinnerNumberModel(100, 1, AutoClickerConfig.MAX_REPEAT_COUNT, 10));
        spinnerRepeat.setPreferredSize(new Dimension(120, 32));
        limit.add(spinnerRepeat);
        JPanel delay = new FlowLayoutPanel();
        delay.add(new JLabel("Contagem antes de iniciar:"));
        spinnerStartDelay = new JSpinner(new SpinnerNumberModel(0, 0, AutoClickerConfig.MAX_START_DELAY, 1));
        spinnerStartDelay.setPreferredSize(new Dimension(70, 32));
        delay.add(spinnerStartDelay);
        delay.add(new JLabel("segundos"));
        content.add(checkInfinite);
        content.add(limit);
        content.add(delay);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createHotkeySection() {
        JPanel card = card("Atalho global");
        JPanel content = new FlowLayoutPanel();
        content.add(new JLabel("Tecla:"));
        textFieldHotkey = new JTextField(config.getHotkeyActivation(), 10);
        textFieldHotkey.setEditable(false);
        textFieldHotkey.setHorizontalAlignment(SwingConstants.CENTER);
        textFieldHotkey.setFont(new Font("Segoe UI", Font.BOLD, 14));
        textFieldHotkey.setForeground(AMBER);
        content.add(textFieldHotkey);
        btnChangeHotkey = new JButton("Alterar");
        btnChangeHotkey.addActionListener(event -> changeHotkey());
        content.add(btnChangeHotkey);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createMacroSection() {
        JPanel card = card("Sequência de ações (macro)");
        checkMacroMode = new JCheckBox("Usar sequência de ações");
        checkMacroMode.addActionListener(event -> {
            presenter.setMacroMode(checkMacroMode.isSelected());
            updateControlAvailability();
        });
        macroTableModel = new MacroTableModel(presenter.getMacroSequence());
        macroTable = new JTable(macroTableModel);
        macroTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        macroTable.setRowHeight(24);
        macroTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane tableScroll = new JScrollPane(macroTable);
        tableScroll.setPreferredSize(new Dimension(400, 135));

        JPanel toolbar = new JPanel(new GridLayout(0, 4, 5, 5));
        JButton addClick = macroButton("+ Clique", event -> beginPositionCapture(point ->
                presenter.getMacroSequence().add(new ClickAction(point.x, point.y,
                        (MouseButton) comboMouseButton.getSelectedItem(), (ClickType) comboClickType.getSelectedItem()))));
        JButton addWait = macroButton("+ Espera", event -> addWaitAction());
        JButton addMove = macroButton("+ Mover", event -> beginPositionCapture(point ->
                presenter.getMacroSequence().add(new ClickAction(point.x, point.y))));
        JButton remove = macroButton("Remover", event -> {
            int row = macroTable.getSelectedRow();
            if (row >= 0) presenter.getMacroSequence().remove(row);
        });
        JButton up = macroButton("↑", event -> moveSelected(-1));
        JButton down = macroButton("↓", event -> moveSelected(1));
        JButton clear = macroButton("Limpar", event -> {
            if (JOptionPane.showConfirmDialog(this, "Limpar toda a sequência?", "Confirmar",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) presenter.getMacroSequence().clear();
        });
        toolbar.add(addClick); toolbar.add(addWait); toolbar.add(addMove); toolbar.add(remove);
        toolbar.add(up); toolbar.add(down); toolbar.add(clear);

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.add(checkMacroMode, BorderLayout.NORTH);
        content.add(tableScroll, BorderLayout.CENTER);
        content.add(toolbar, BorderLayout.SOUTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatusAndControlFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor("Component.borderColor")),
                BorderFactory.createEmptyBorder(10, 16, 12, 16)));
        labelStatus = centeredLabel("Parado", new Font("Segoe UI", Font.BOLD, 15));
        labelActionCount = centeredLabel("Ações executadas: 0", LABEL_FONT);
        labelElapsed = centeredLabel("Tempo: 0s", LABEL_FONT);
        labelRate = centeredLabel("Taxa: —", LABEL_FONT);
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        progressBar.setVisible(false);
        btnToggle = new JButton("Iniciar");
        btnToggle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnToggle.setForeground(Color.WHITE);
        btnToggle.setBackground(GREEN);
        btnToggle.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btnToggle.addActionListener(event -> requestToggle());
        labelHotkeyHint = centeredLabel("Pressione F6 para iniciar/parar", new Font("Segoe UI", Font.ITALIC, 11));
        footer.add(labelStatus); footer.add(labelActionCount); footer.add(labelElapsed); footer.add(labelRate);
        footer.add(Box.createVerticalStrut(5)); footer.add(progressBar); footer.add(Box.createVerticalStrut(7));
        footer.add(btnToggle); footer.add(Box.createVerticalStrut(4)); footer.add(labelHotkeyHint);
        return footer;
    }

    private void requestToggle() {
        presenter.toggleClicking();
    }

    private boolean commitEditorsToConfig() {
        try {
            spinnerInterval.commitEdit(); spinnerRepeat.commitEdit(); spinnerStartDelay.commitEdit();
            spinnerFixX.commitEdit(); spinnerFixY.commitEdit();
            config.setClickIntervalSeconds(((Number) spinnerInterval.getValue()).doubleValue());
            config.setMouseButton((MouseButton) comboMouseButton.getSelectedItem());
            config.setClickType((ClickType) comboClickType.getSelectedItem());
            config.setInfiniteRepeat(checkInfinite.isSelected());
            config.setRepeatCount(((Number) spinnerRepeat.getValue()).intValue());
            config.setStartDelaySeconds(((Number) spinnerStartDelay.getValue()).intValue());
            config.setUseFixedPosition(checkFixedPos.isSelected());
            config.setFixX(((Number) spinnerFixX.getValue()).intValue());
            config.setFixY(((Number) spinnerFixY.getValue()).intValue());
            if (config.isUseFixedPosition() && !isPointOnScreen(new Point(config.getFixX(), config.getFixY()))) {
                throw new IllegalArgumentException("A posição fixa não pertence a nenhum monitor conectado.");
            }
            config.validate();
            return true;
        } catch (ParseException | IllegalArgumentException e) {
            showMessage(e.getMessage() == null ? "Revise os valores informados." : e.getMessage(), JOptionPane.WARNING_MESSAGE);
            return false;
        }
    }

    private void beginPositionCapture(Consumer<Point> onCaptured) {
        if (executionState != ExecutionState.STOPPED || capturingPosition) return;
        capturingPosition = true;
        refreshActivationSuspension();
        updateControlAvailability();
        captureDialog = new JDialog(this, "Capturar posição", false);
        JLabel countdown = centeredLabel("Posicione o cursor: 3", new Font("Segoe UI", Font.BOLD, 16));
        JButton cancel = new JButton("Cancelar");
        cancel.addActionListener(event -> cancelPositionCapture());
        captureDialog.setLayout(new BorderLayout(8, 8));
        captureDialog.add(countdown, BorderLayout.CENTER);
        captureDialog.add(cancel, BorderLayout.SOUTH);
        captureDialog.setSize(280, 120);
        captureDialog.setAlwaysOnTop(true);
        captureDialog.setLocationRelativeTo(this);
        captureDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        captureDialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { cancelPositionCapture(); }
        });
        final int[] remaining = {3};
        captureTimer = new Timer(1000, event -> {
            remaining[0]--;
            if (remaining[0] > 0) countdown.setText("Posicione o cursor: " + remaining[0]);
            else {
                Point point = pointerLocation();
                finishPositionCapture();
                if (point == null || !isPointOnScreen(point)) {
                    showMessage("Não foi possível obter uma posição válida do cursor.", JOptionPane.WARNING_MESSAGE);
                } else {
                    onCaptured.accept(point);
                }
            }
        });
        captureTimer.setRepeats(true);
        captureTimer.start();
        captureDialog.setVisible(true);
    }

    private Point pointerLocation() {
        PointerInfo info = MouseInfo.getPointerInfo();
        return info == null ? null : info.getLocation();
    }

    private static boolean isPointOnScreen(Point point) {
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            for (GraphicsConfiguration configuration : device.getConfigurations()) {
                if (configuration.getBounds().contains(point)) return true;
            }
        }
        return false;
    }

    private void cancelPositionCapture() { finishPositionCapture(); }
    private void finishPositionCapture() {
        if (captureTimer != null) captureTimer.stop();
        if (captureDialog != null) captureDialog.dispose();
        captureTimer = null; captureDialog = null; capturingPosition = false;
        refreshActivationSuspension();
        updateControlAvailability();
    }

    private void changeHotkey() {
        if (executionState != ExecutionState.STOPPED || capturingPosition) return;
        capturingHotkey = true;
        refreshActivationSuspension();
        JDialog dialog = new JDialog(this, "Novo atalho", true);
        JLabel label = centeredLabel("Pressione uma tecla...", LABEL_FONT);
        JButton cancel = new JButton("Cancelar");
        Runnable close = () -> {
            hotkeyListener.cancelCapture();
            capturingHotkey = false;
            refreshActivationSuspension();
            dialog.dispose();
        };
        cancel.addActionListener(event -> close.run());
        dialog.setLayout(new BorderLayout(8, 8)); dialog.add(label, BorderLayout.CENTER); dialog.add(cancel, BorderLayout.SOUTH);
        dialog.setSize(300, 130); dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { close.run(); } });
        hotkeyListener.beginCapture((code, name) -> {
            config.setHotkey(code, name);
            textFieldHotkey.setText(name);
            labelHotkeyHint.setText("Pressione " + name + " para iniciar/parar");
            capturingHotkey = false;
            refreshActivationSuspension();
            dialog.dispose();
        });
        dialog.setVisible(true);
    }

    private void saveProfile() {
        if (!commitEditorsToConfig()) return;
        String name;
        try { name = ConfigRepository.validateName(selectedProfileName()); }
        catch (IllegalArgumentException e) { showMessage(e.getMessage(), JOptionPane.WARNING_MESSAGE); return; }
        runBackground(() -> configRepository.exists(name), exists -> {
            if (exists && JOptionPane.showConfirmDialog(this, "Sobrescrever o perfil \"" + name + "\"?",
                    "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            AutoClickerConfig snapshot = config.copy();
            runBackground(() -> { configRepository.save(name, snapshot); return name; }, saved -> loadProfileNames());
        });
    }

    private void loadProfile() {
        String name;
        try { name = ConfigRepository.validateName(selectedProfileName()); }
        catch (IllegalArgumentException e) { showMessage(e.getMessage(), JOptionPane.WARNING_MESSAGE); return; }
        runBackground(() -> configRepository.load(name), loaded -> {
            if (loaded.isEmpty()) { showMessage("Perfil não encontrado.", JOptionPane.INFORMATION_MESSAGE); return; }
            ConfigRepository.LoadedProfile profile = loaded.get();
            config.copyFrom(profile.getConfig());
            syncUIFromConfig();
            if (profile.isHotkeyReset()) showMessage("O atalho antigo não foi reconhecido e voltou para F6.", JOptionPane.WARNING_MESSAGE);
        });
    }

    private void deleteProfile() {
        String name;
        try { name = ConfigRepository.validateName(selectedProfileName()); }
        catch (IllegalArgumentException e) { showMessage(e.getMessage(), JOptionPane.WARNING_MESSAGE); return; }
        if (JOptionPane.showConfirmDialog(this, "Excluir o perfil \"" + name + "\"?", "Confirmar",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        runBackground(() -> configRepository.delete(name), removed -> loadProfileNames());
    }

    private void loadProfileNames() {
        runBackground(configRepository::listNames, names -> {
            String selected = selectedProfileName();
            comboProfiles.removeAllItems();
            names.forEach(comboProfiles::addItem);
            if (!selected.isBlank()) comboProfiles.setSelectedItem(selected);
        });
    }

    private <T> void runBackground(Callable<T> task, Consumer<T> success) {
        backgroundOperations++;
        refreshActivationSuspension();
        updateControlAvailability();
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return task.call(); }
            @Override protected void done() {
                backgroundOperations--;
                try { success.accept(get()); }
                catch (Exception e) {
                    Throwable cause = e.getCause() == null ? e : e.getCause();
                    showMessage(cause.getMessage() == null ? "A operação não pôde ser concluída." : cause.getMessage(), JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshActivationSuspension();
                    updateControlAvailability();
                }
            }
        }.execute();
    }

    private void syncUIFromConfig() {
        spinnerInterval.setValue(config.getClickIntervalSeconds());
        comboMouseButton.setSelectedItem(config.getMouseButton());
        comboClickType.setSelectedItem(config.getClickType());
        spinnerRepeat.setValue(config.getRepeatCount());
        checkInfinite.setSelected(config.isInfiniteRepeat());
        spinnerStartDelay.setValue(config.getStartDelaySeconds());
        checkFixedPos.setSelected(config.isUseFixedPosition());
        spinnerFixX.setValue(config.getFixX()); spinnerFixY.setValue(config.getFixY());
        textFieldHotkey.setText(config.getHotkeyActivation());
        labelHotkeyHint.setText("Pressione " + config.getHotkeyActivation() + " para iniciar/parar");
        updateControlAvailability();
    }

    private void addWaitAction() {
        String value = JOptionPane.showInputDialog(this, "Duração da espera (ms):", "Adicionar espera", JOptionPane.PLAIN_MESSAGE);
        if (value == null || value.isBlank()) return;
        try { presenter.getMacroSequence().add(new ClickAction(Integer.parseInt(value.trim()))); }
        catch (IllegalArgumentException e) { showMessage("Informe um número inteiro não negativo.", JOptionPane.WARNING_MESSAGE); }
    }

    private void moveSelected(int direction) {
        int row = macroTable.getSelectedRow();
        int target = row + direction;
        if (row < 0 || target < 0 || target >= presenter.getMacroSequence().size()) return;
        if (direction < 0) presenter.getMacroSequence().moveUp(row); else presenter.getMacroSequence().moveDown(row);
        macroTable.setRowSelectionInterval(target, target);
    }

    private void shutdown() {
        cancelPositionCapture();
        hotkeyListener.cancelCapture();
        presenter.shutdown();
        hotkeyListener.unregister();
        dispose();
    }

    @Override public void onExecutionStateChanged(ExecutionState state, boolean macroMode) {
        executionState = state;
        switch (state) {
            case STOPPED: labelStatus.setText("Parado"); labelStatus.setForeground(Color.GRAY); break;
            case COUNTDOWN: labelStatus.setText("Preparando..."); labelStatus.setForeground(AMBER); break;
            case RUNNING: labelStatus.setText(macroMode ? "Macro em execução" : "Executando"); labelStatus.setForeground(GREEN); break;
            case STOPPING: labelStatus.setText("Parando..."); labelStatus.setForeground(AMBER); break;
            default: break;
        }
        btnToggle.setText(state == ExecutionState.STOPPED ? "Iniciar" : state == ExecutionState.STOPPING ? "Parando..." : "Parar");
        btnToggle.setBackground(state == ExecutionState.STOPPED ? GREEN : RED);
        updateControlAvailability();
    }

    @Override public void onStatusTick(SessionMetrics metrics, long total, boolean infinite, boolean macroMode) {
        long count = metrics.getCompletedActions();
        labelActionCount.setText((macroMode ? "Ciclos concluídos: " : "Ações executadas: ") + count
                + (infinite ? "" : " / " + total));
        labelElapsed.setText(String.format("Tempo: %.1fs", metrics.getElapsedSeconds()));
        labelRate.setText(metrics.getActionsPerSecond() > 0
                ? String.format(macroMode ? "Taxa: %.1f ciclos/s" : "Taxa: %.1f ações/s", metrics.getActionsPerSecond()) : "Taxa: —");
        progressBar.setVisible(!infinite);
        if (!infinite) {
            progressBar.setValue(total > 0 ? (int) Math.min(100, count * 100 / total) : 0);
            progressBar.setString(count + " / " + total);
        }
    }

    @Override public void onCountdown(int secondsLeft) {
        labelStatus.setText("Iniciando em " + secondsLeft + "s...");
        labelStatus.setForeground(AMBER);
    }

    @Override public void onActionIndexChanged(int index) {
        if (index >= 0 && index < macroTable.getRowCount()) {
            macroTable.setRowSelectionInterval(index, index);
            macroTable.scrollRectToVisible(macroTable.getCellRect(index, 0, true));
        }
    }

    @Override public void onExecutionFinished(ExecutionEvent.StopReason reason) {
        if (reason == ExecutionEvent.StopReason.COMPLETED) {
            labelStatus.setText("Concluído");
            labelStatus.setForeground(GREEN);
        } else if (reason == ExecutionEvent.StopReason.FAILED) {
            labelStatus.setText("Falha");
            labelStatus.setForeground(RED);
        }
    }

    @Override public void onExecutionError(String message, Throwable error) {
        String detail = error == null || error.getMessage() == null ? message : message + "\n" + error.getMessage();
        showMessage(detail, JOptionPane.ERROR_MESSAGE);
    }

    private void updateControlAvailability() {
        boolean idle = executionState == ExecutionState.STOPPED && !capturingPosition && !capturingHotkey && backgroundOperations == 0;
        spinnerInterval.setEnabled(idle); comboMouseButton.setEnabled(idle); comboClickType.setEnabled(idle);
        checkInfinite.setEnabled(idle); spinnerRepeat.setEnabled(idle && !checkInfinite.isSelected());
        spinnerStartDelay.setEnabled(idle); checkFixedPos.setEnabled(idle);
        spinnerFixX.setEnabled(idle && checkFixedPos.isSelected()); spinnerFixY.setEnabled(idle && checkFixedPos.isSelected());
        btnCapture.setEnabled(idle && checkFixedPos.isSelected()); btnChangeHotkey.setEnabled(idle);
        checkMacroMode.setEnabled(idle); macroTable.setEnabled(idle && checkMacroMode.isSelected());
        macroButtons.forEach(button -> button.setEnabled(idle && checkMacroMode.isSelected()));
        setProfileControlsEnabled(idle);
        boolean activeAndStoppable = executionState == ExecutionState.COUNTDOWN || executionState == ExecutionState.RUNNING;
        btnToggle.setEnabled(activeAndStoppable || idle);
    }

    private void refreshActivationSuspension() {
        presenter.setActivationSuspended(capturingPosition || capturingHotkey || backgroundOperations > 0);
    }

    private void setProfileControlsEnabled(boolean enabled) {
        comboProfiles.setEnabled(enabled); btnProfileSave.setEnabled(enabled); btnProfileLoad.setEnabled(enabled); btnProfileDelete.setEnabled(enabled);
    }

    private String selectedProfileName() {
        Object value = comboProfiles.getEditor().getItem();
        return value == null ? "" : value.toString().trim();
    }

    private JButton macroButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text); button.addActionListener(listener); macroButtons.add(button); return button;
    }

    private static JSpinner coordinateSpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, Integer.MIN_VALUE, Integer.MAX_VALUE, 1));
        spinner.setPreferredSize(new Dimension(90, 30)); return spinner;
    }

    private static DefaultListCellRenderer enumRenderer() {
        return new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof MouseButton) setText(((MouseButton) value).getDisplayName());
                if (value instanceof ClickType) setText(((ClickType) value).getDisplayName());
                return this;
            }
        };
    }

    private static JLabel centeredLabel(String text, Font font) {
        JLabel label = new JLabel(text); label.setFont(font); label.setAlignmentX(Component.CENTER_ALIGNMENT); label.setHorizontalAlignment(SwingConstants.CENTER); return label;
    }

    private JPanel card(String title) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        JLabel heading = new JLabel(title); heading.setFont(SECTION_FONT); panel.add(heading, BorderLayout.NORTH); return panel;
    }

    private void showMessage(String message, int type) { JOptionPane.showMessageDialog(this, message, "AutoClicker Pro", type); }

    @SuppressWarnings("serial")
    private static final class FlowLayoutPanel extends JPanel {
        FlowLayoutPanel() { super(new FlowLayout(FlowLayout.LEFT, 8, 5)); }
    }

    @SuppressWarnings("serial")
    private static final class MacroTableModel extends AbstractTableModel {
        private final String[] columns = {"#", "Tipo", "Botão / Clique", "Posição", "Resumo"};
        private final MacroSequence sequence;
        MacroTableModel(MacroSequence sequence) {
            this.sequence = sequence;
            sequence.addPropertyChangeListener(event -> SwingUtilities.invokeLater(this::fireTableDataChanged));
        }
        @Override public int getRowCount() { return sequence.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int column) { return columns[column]; }
        @Override public Object getValueAt(int row, int column) {
            ClickAction action = sequence.get(row);
            switch (column) {
                case 0: return row + 1;
                case 1: return action.getActionType().getDisplayName();
                case 2: return action.getActionType() == ClickAction.ActionType.CLICK
                        ? action.getMouseButton().getDisplayName() + " / " + action.getClickType().getDisplayName()
                        : action.getActionType() == ClickAction.ActionType.WAIT ? action.getWaitMs() + " ms" : "—";
                case 3: return action.getActionType() == ClickAction.ActionType.WAIT ? "—" : "(" + action.getX() + ", " + action.getY() + ")";
                case 4: return action.getSummary();
                default: return "";
            }
        }
    }

    public AutoClickerConfig getConfig() { return config; }
    public AutoClickerEngine getEngine() { return engine; }
    public void startClicking() { requestToggle(); }
    public void stopClicking() { presenter.stopClicking(); }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.d3d", "true");
        System.setProperty("sun.java2d.dpiaware", "true");
        SwingUtilities.invokeLater(() -> {
            try {
                AutoClickerUI ui = new AutoClickerUI();
                ui.setVisible(true);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null, "Não foi possível iniciar o AutoClicker Pro.\n" + e.getMessage(),
                        "AutoClicker Pro", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
