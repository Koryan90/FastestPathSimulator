package Model;

import java.util.ArrayList;

import Model.ArenaTemplate.CellState;

public class FastPathModel {
	
	public enum Cell {
		OBSTACLE,
		EMPTY,
		ROBOT,
		ROBOT_DIRECTION,
		UNEXMPLORED,
		PATH
	}
	
	public class SimulatorException extends Exception{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private int ID;
		private String msg;
		public String toString(){
			return "" + ID + ": "+ msg;
		}
		
		public  SimulatorException(int ID,String msg){
			this.ID = ID;
			this.msg = msg;
		}
	}
	
	private CustomizedArena arenaMap;
	private Robot robot;
	private Cell[][] currentStatus;
	
	private ArrayList<Action> fastestPath = new ArrayList<>();
	//All the action from 0 to actionIndex - 1 has been executed.
	//FastestPath[action] has not yet be executed.
	private int actionIndex = 0;
	
	private int lowerLeftGoalRowID;
	private int lowerLeftGoalColID;
	
	private int lowerLeftStartRowID;
	private int lowerLeftStartColID;
	private Direction startDirection;
		
	
	
	public FastPathModel(CustomizedArena arenaMap, 
						int lowerLeftStartRowID,
						int lowerLeftStartColID,
						Direction startDirection,
			
						int lowerLeftGoalRowID,
						int lowerLeftGoalColID, 
						int robotDiameterInCellCount) throws SimulatorException {
		super();
		
		
		this.arenaMap = arenaMap;
	
		if(obstacleInArea(lowerLeftStartRowID,lowerLeftStartColID,robotDiameterInCellCount)){
			throw new SimulatorException(1, "There exists obstacle in the START position");
		}
		if(obstacleInArea(lowerLeftGoalRowID,lowerLeftGoalColID,robotDiameterInCellCount)){
			throw new SimulatorException(1, "There exists obstacle in the GOAL position");
		}
		
		this.lowerLeftGoalRowID = lowerLeftGoalRowID;
		this.lowerLeftGoalColID = lowerLeftGoalColID;
		this.lowerLeftStartRowID = lowerLeftStartRowID;
		this.lowerLeftStartColID = lowerLeftStartColID;
		this.startDirection = startDirection;
		
		this.robot = new Robot(lowerLeftStartRowID, lowerLeftStartColID, robotDiameterInCellCount,startDirection);
		this.currentStatus = new Cell[arenaMap.getRowCount()][arenaMap.getColumnCount()];
		if(!computeFastestPath()){
			throw new SimulatorException(2, "No Path can be found");
		}
		updateStatus();
	}

	private boolean obstacleInArea(int lowerLeftRowID,
			int lowerLeftColID, int span) {

		for(int rowID = 0;rowID < span;rowID++){
			for(int colID = 0;colID < span;colID++){
				if(this.arenaMap.getCells()
						[lowerLeftRowID - rowID][lowerLeftColID + colID] 
						== ArenaTemplate.CellState.OBSTACLE){
					return true;
				}
			}
		}
		return false;
	}
	
	public int getArenaRowCount(){
		return this.arenaMap.getRowCount();
	}
	
	public int getArenaColCount(){
		return this.arenaMap.getColumnCount();
	}

	public boolean hasNextStep(){
		return this.actionIndex < this.fastestPath.size();
	}
	
	public boolean hasPreStep(){
		return this.actionIndex > 0;
	}
	
	
	private boolean computeFastestPath(){
		//TODO 
		//The below actions are only for testing
		
		int robotDiameterInCellNum = this.robot.getDiameterInCellNum();
		
		for(int i = 0;
				i < this.getArenaRowCount() - robotDiameterInCellNum;
				i++){
			this.fastestPath.add(Action.MOVE_FORWARD);
		}
		this.fastestPath.add(Action.TURN_RIGHT);
		for(int i = 0;
				i < this.getArenaColCount() - robotDiameterInCellNum;
				i++){
			this.fastestPath.add(Action.MOVE_FORWARD);
		}
		return true;
	}
	
	public Cell getCellStatus(int rowID, int colID){
		return this.currentStatus[rowID][colID];
	}
	
	//return whether there exists movement after this
	public void forward(){
		if(actionIndex >= 0 && actionIndex < fastestPath.size()){
			this.robot.move(fastestPath.get(actionIndex));
			actionIndex++;
			updateStatus();
			
			}
		}
	
	
	//return whether there exists movement before this
	public  void backward(){
		if(actionIndex >= 1 && actionIndex <= fastestPath.size()){
			actionIndex--;
			this.robot.move(Action.opposite(fastestPath.get(actionIndex)));
			updateStatus();
		}
		
	}
	
	private void updateStatus(){
		
		updateForArenaMap();
		updateForRobot();
		updateForPath();
	}

	private void updateForPath() {
		Direction direction = this.startDirection;
		int lowerLeftRowID = this.lowerLeftStartRowID;
		int lowerLeftColID = this.lowerLeftStartColID;
		int robotDiameterInCellNum = this.robot.getDiameterInCellNum();
		
		for(int actID = 0;actID < this.actionIndex;actID++){
			Action act = this.fastestPath.get(actID);
			
			if((direction.equals(Direction.UP) && act.equals(Action.MOVE_FORWARD))
					||(direction.equals(Direction.DOWN) && act.equals(Action.DRAW_BACK))){
				
				//MOVE UPWARD
				int rowID = lowerLeftRowID;
				int colID = lowerLeftColID;
				for(int offset = 0;offset < robotDiameterInCellNum; offset++){
					this.currentStatus[rowID][colID + offset] =  Cell.PATH;
				}
				lowerLeftRowID--;
			}else if((direction.equals(Direction.UP) && act.equals(Action.DRAW_BACK))
					||(direction.equals(Direction.DOWN) && act.equals(Action.MOVE_FORWARD))){
				
				//MOVE DOWNWARD
				//Draw the DIRECTION CELL at the top of robot
				int rowID = lowerLeftRowID - robotDiameterInCellNum + 1;
				int colID = lowerLeftColID;

				for(int offset = 0;offset < robotDiameterInCellNum; offset++){
					this.currentStatus[rowID][colID + offset] =  Cell.PATH;
				}
				lowerLeftRowID++;
			}else if((direction.equals(Direction.LEFT) && act.equals(Action.MOVE_FORWARD))
					||(direction.equals(Direction.RIGHT) && act.equals(Action.DRAW_BACK))){
				
				//MOVE TO LEFT	
				//Draw the path on right side of the robot
				int rowID = lowerLeftRowID;
				int colID = lowerLeftColID + robotDiameterInCellNum - 1;

				for(int offset = 0;offset < robotDiameterInCellNum; offset++){
					this.currentStatus[rowID - offset][colID] =  Cell.PATH;
				}
				lowerLeftColID--;
			}else if((direction.equals(Direction.LEFT) && act.equals(Action.DRAW_BACK))
					||(direction.equals(Direction.RIGHT) && act.equals(Action.MOVE_FORWARD))){
				
				//MOVE TO RIGHT	
				//Draw the path on left side of the robot
				int rowID = lowerLeftRowID;
				int colID = lowerLeftColID;

				for(int offset = 0;offset < robotDiameterInCellNum; offset++){
					this.currentStatus[rowID - offset][colID] =  Cell.PATH;
				}
				lowerLeftColID++;
			}
			
			direction = act.directionAfterAction(direction);
		}
	}

	private void updateForRobot() {
		int robotDiameterInCellNum = this.robot.getDiameterInCellNum();
		int cellRowIndex, cellColIndex;
		for(int rowOffset = 0;rowOffset < robotDiameterInCellNum;rowOffset++){
			cellRowIndex = this.robot.getLowerLeftRowIndex() - rowOffset;
			for(int colOffset = 0;colOffset < robotDiameterInCellNum;colOffset++){
				cellColIndex = this.robot.getLowerLeftColIndex() + colOffset;
				
				assert(this.currentStatus[cellRowIndex][cellColIndex] != Cell.OBSTACLE);
				this.currentStatus[cellRowIndex][cellColIndex] = Cell.ROBOT;		
			}
		}
		
		//Draw the Direction Cell
		
		if(this.robot.getCurrentDirection().equals(Direction.LEFT)){
		
			cellRowIndex = this.robot.getLowerLeftRowIndex();
			cellColIndex = this.robot.getLowerLeftColIndex();

			for(int offset = 0;offset < robotDiameterInCellNum;offset++){
				this.currentStatus[cellRowIndex][cellColIndex] = Cell.ROBOT_DIRECTION;
				cellRowIndex --;
			}
		}else if(this.robot.getCurrentDirection().equals(Direction.RIGHT)){
		
			cellRowIndex = this.robot.getLowerLeftRowIndex();
			cellColIndex = this.robot.getLowerLeftColIndex() + robotDiameterInCellNum - 1;
			
			for(int offset = 0;offset < robotDiameterInCellNum;offset++){
				this.currentStatus[cellRowIndex][cellColIndex] = Cell.ROBOT_DIRECTION;
				cellRowIndex --;
			}
		}else if(this.robot.getCurrentDirection().equals(Direction.UP)){
			cellRowIndex = this.robot.getLowerLeftRowIndex() - robotDiameterInCellNum + 1;
			cellColIndex = this.robot.getLowerLeftColIndex();

			for(int offset = 0;offset < robotDiameterInCellNum;offset++){
				this.currentStatus[cellRowIndex][cellColIndex] = Cell.ROBOT_DIRECTION;
				cellColIndex ++;
			}
			
		}else if(this.robot.getCurrentDirection().equals(Direction.DOWN)){
			cellRowIndex = this.robot.getLowerLeftRowIndex();
			cellColIndex = this.robot.getLowerLeftColIndex();

			for(int offset = 0;offset < robotDiameterInCellNum;offset++){
				this.currentStatus[cellRowIndex][cellColIndex] = Cell.ROBOT_DIRECTION;
				cellColIndex ++;
			}
		}
		
	}

	private void updateForArenaMap() {
		for(int rowID = 0;rowID < this.arenaMap.getRowCount();rowID++){
			for(int colID = 0;colID < this.arenaMap.getColumnCount();colID++){
				if(this.arenaMap.getCells()[rowID][colID] == CellState.OBSTACLE){
					this.currentStatus[rowID][colID] = Cell.OBSTACLE;
				}else if(this.arenaMap.getCells()[rowID][colID] == CellState.EMPTY){
					this.currentStatus[rowID][colID] = Cell.EMPTY;
				}else{
					assert(false):"This arena should not contain any UNEXPLORED CELL";
				}
			}
		}
	}
	
	public int getCurrentTurnCount(){
		int count = 0;
		for (int actionID = 0;actionID < actionIndex;actionID++){
			if(fastestPath.get(actionID) == Action.TURN_LEFT 
				|| fastestPath.get(actionID) == Action.TURN_RIGHT){
				count++;
			}
		}
		return count;
	}
	
	public int getCurrentStepCount(){
		int count = 0;
		for (int actionID = 0;actionID < actionIndex;actionID++){
			if(fastestPath.get(actionID) == Action.MOVE_FORWARD 
				|| fastestPath.get(actionID) == Action.DRAW_BACK){
				count++;
			}
		}
		return count;
	}
	
	public void reset(){
		this.robot.setCurrentDirection(startDirection);
		this.robot.setLowerLeftRowIndex(lowerLeftStartRowID);
		this.robot.setLowerLeftColIndex(lowerLeftStartColID);
		this.actionIndex = 0;
		this.updateStatus();
	}
	
}
