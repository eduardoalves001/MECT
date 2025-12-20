library IEEE;
use IEEE.std_logic_1164.all;

entity or_gate_3 is
  port(input0  : in  std_logic;
       input1  : in  std_logic;
       input2  : in  std_logic;
       output0 : out std_logic);
end or_gate_3;

architecture behavioral of or_gate_3 is
begin
  output0 <= transport input0 or input1 or input2 after 15 ps;
end behavioral;
