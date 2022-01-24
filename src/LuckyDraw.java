import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class LuckyDraw extends KeyAdapter {

	public static int WINDOW_WIDTH = 0;
	public static int WINDOW_HEIGHT = 0;

	String resultFormat = "%s 抽中 %s";
	String title = "富宇文匯社區專用抽籤程式";
	String font = "微軟正黑體";
	float delay = 1;
	int itemCols = 10;

	final JFrame jframe = new JFrame();
	Container contentPane = null;

	JPanel leftPanel = null;
	JPanel rightPanel = null;

	JButton startButton = null;
	JPanel startPanel = null;
	JScrollPane tablePanel = null;

	JPanel itemPanel = null;
	JPanel drawPanel = null;
	JLabel drawLabel = null;

	DefaultTableModel tableModel = null;
	JTable resultTable = null;

	ArrayList<User> userList = new ArrayList<User>();
	ArrayList<Item> itemList = new ArrayList<Item>();
	HashMap<String, String> itemResults = new HashMap<String, String>();
	
	public void createUI() {
		loadData();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		WINDOW_WIDTH = (int) screenSize.getWidth();
		WINDOW_HEIGHT = (int) screenSize.getHeight();

		contentPane = jframe.getContentPane();
		contentPane.setLayout(new BorderLayout());

		startPanel = new JPanel();
		startPanel.setLayout(new BorderLayout());
		startButton = new JButton("START");
		startPanel.add(startButton, BorderLayout.CENTER);

		itemPanel = new JPanel();

		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				startButton.setEnabled(false);
				startDraw();
			}
		});

		tableModel = new DefaultTableModel();
		tableModel.addColumn("User");
		tableModel.addColumn("Item");

		resultTable = new JTable(tableModel);
		resultTable.setRowHeight(40);
		resultTable.setFont(new Font(font, Font.BOLD, 40));
		tablePanel = new JScrollPane(resultTable);
		tablePanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		tablePanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

		leftPanel = new JPanel();
		leftPanel.setLayout(new BorderLayout());
		leftPanel.add(tablePanel, BorderLayout.CENTER);
		leftPanel.add(startPanel, BorderLayout.SOUTH);
		leftPanel.setBorder(BorderFactory.createTitledBorder("Information"));

		rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());

		drawPanel = new JPanel();
		drawPanel.setLayout(new BorderLayout());
		drawLabel = new JLabel("", SwingConstants.CENTER);

		drawLabel.setFont(getFont(font, Font.BOLD, WINDOW_WIDTH / 3));

		drawLabel.setText("Ready");
		drawPanel.add(drawLabel, BorderLayout.CENTER);

		rightPanel.add(drawPanel, BorderLayout.CENTER);
		rightPanel.add(itemPanel, BorderLayout.SOUTH);

		rightPanel.setBorder(BorderFactory.createTitledBorder("Draw"));

		contentPane.add(leftPanel, BorderLayout.WEST);
		contentPane.add(rightPanel, BorderLayout.CENTER);

		jframe.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				exitConfirm();
			}
		});

		jframe.setTitle(title);
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
		jframe.pack();

		jframe.setVisible(true);

		setComponentSize();

	}

	// 設定合適字型大小
	public Font getFont(String name, int style, int width) {
		int size = width;
		Boolean up = null;
		while (true) {
			Font font = new Font(name, style, size);
			int testWidth = rightPanel.getFontMetrics(font).stringWidth(resultFormat);
			if (testWidth < width && up != Boolean.FALSE) {
				size++;
				up = Boolean.TRUE;
			} else if (testWidth > width && up != Boolean.TRUE) {
				size--;
				up = Boolean.FALSE;
			} else {
				return font;
			}
		}
	}
	
	// 儲存抽籤結果寫成檔案
	private void saveData() {
		try {
			File path = new File("Output");
			if (!path.exists()) {
				path.mkdir();
			}
			String resultFile = path + "/Reuslts_Item.csv";
			FileWriter fw = new FileWriter(resultFile);

			Component[] components = itemPanel.getComponents();
			for (int i = 0; i < components.length; i++) {
				JButton component = (JButton) components[i];
				String item = component.getText();
				String user = itemResults.get(item) == null ? "" : itemResults.get(item);
				if (!item.isEmpty()) {
					fw.write(String.format("%s,%s\n", user, item));
				}
			}
			fw.close();

			resultFile = path + "/Reuslts_User.csv";
			fw = new FileWriter(resultFile);
			for (int i = 0; i < tableModel.getRowCount(); i++) {
				String user = Objects.toString(tableModel.getValueAt(i, 0), "").trim();
				String item = Objects.toString(tableModel.getValueAt(i, 1), "").trim();
				if (!user.isEmpty() && !item.isEmpty()) {
					fw.write(String.format("%s,%s\n", user, item));
				}
			}
			fw.close();

			JOptionPane.showConfirmDialog(null, "抽籤完畢，檔案已儲存至" + resultFile, "", JOptionPane.DEFAULT_OPTION);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 從檔案讀取名單
	private void loadData() {
		String userFile = "Users.csv";
		String itemFile = "Items.csv";
		BufferedReader br;
		String line;
		try {
			if (new File(userFile).exists() && new File(itemFile).exists()) {
				br = new BufferedReader(new FileReader(userFile));
				while ((line = br.readLine()) != null) {
					User user = new User(line);
					user.index = userList.size();
					userList.add(user);
				}
				br = new BufferedReader(new FileReader(itemFile));
				while ((line = br.readLine()) != null) {
					Item item = new Item(line);
					item.index = itemList.size();
					itemList.add(item);
				}
			} else {
				JOptionPane.showConfirmDialog(null, "User file or item file does not exist.", "",
						JOptionPane.DEFAULT_OPTION);
				System.exit(0);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 抽籤
	private void startDraw() {
		Random rand = new Random(); // instance of random class
		Thread updateUIThread = new Thread(() -> {
			try {
				while (userList.size() > 0) {
					// 從箱子取出用戶
					User user = userList.get(rand.nextInt(userList.size()));
					// 從籤桶移除用戶
					userList.remove(user);
					// 從箱子取出物件
					Item item = itemList.get(rand.nextInt(itemList.size()));
					// 將物件相對應按鈕變暗
					item.button.setEnabled(false);
					// 從簽筒移除物件
					itemList.remove(item);
					// 在表格顯示抽中結果
					tableModel.setValueAt(item.name, user.index, 1);
					// 反白抽中的用戶
					resultTable.changeSelection(user.index, 0, false, false);
					// 將結果顯示在左邊視窗
					String result = String.format(resultFormat, user.name, item.name);
					drawLabel.setText(result);
					// 儲存結果
					itemResults.put(item.name, user.name);
					// 重新整理畫面
					jframe.repaint();
					// 停頓delay秒
					Thread.sleep((int) delay * 1000);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				System.out.println(ex);
			}
			saveData();
		});
		updateUIThread.start();
	}

	private void exitConfirm() {
		int optionType = JOptionPane.YES_NO_OPTION;
		String message = "Are you sure to exit the program?";
		int confirmResult = JOptionPane.showConfirmDialog(jframe, message, "Confirm", optionType,
				JOptionPane.INFORMATION_MESSAGE);
		if (confirmResult == JOptionPane.YES_OPTION) {
			System.exit(0);
		}
	}

	// 設定元件大小
	private void setComponentSize() {
		for (User user : userList) {
			tableModel.addRow(new Object[] { user.name, "" });
		}

		itemPanel.setLayout(new GridLayout(0, itemCols));
		for (int i = 0; i < itemList.size(); i++) {
			Item item = itemList.get(i);
			item.button = new JButton(item.name);
			itemPanel.add(item.button);
		}

		itemPanel.setPreferredSize(new Dimension(itemPanel.getWidth(), WINDOW_HEIGHT / 4));
		startPanel.setPreferredSize(new Dimension(startPanel.getWidth(), WINDOW_HEIGHT / 10));
		jframe.repaint();
	}

	public static void main(String args[]) throws ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				LuckyDraw app = new LuckyDraw();
				app.createUI();
			}
		});

	}

	class User {
		protected String name;
		protected int index;

		public User(String name) {
			this.name = name;
		}
	}

	class Item {
		protected String name;
		protected int index;
		protected JButton button;

		public Item(String name) {
			this.name = name;
		}
	}
}