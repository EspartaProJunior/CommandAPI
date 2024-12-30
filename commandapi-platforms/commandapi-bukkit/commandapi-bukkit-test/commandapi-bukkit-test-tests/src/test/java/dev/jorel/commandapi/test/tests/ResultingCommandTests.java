package dev.jorel.commandapi.test.tests;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.jorel.commandapi.test.TestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import dev.jorel.commandapi.CommandAPICommand;

/**
 * Tests for commands with resulting command executors
 */
@SuppressWarnings("unused")
class ResultingCommandTests extends TestBase {
	
	/*********
	 * Setup *
	 *********/
	
	@BeforeEach
	public void setUp() {
		super.setUp();
	}

	@AfterEach
	public void tearDown() {
		super.tearDown();
	}
	
	/*********
	 * Tests *
	 *********/

	@Test
	void testResultingWorkSuccess() {
		new CommandAPICommand("test")
			.executes((sender, args) -> {
				return 10;
			})
			.register();
		
		PlayerMock player = server.addPlayer();
		
		int result = server.dispatchBrigadierCommand(player, "test");
		
		assertEquals(10, result);
	}

}
