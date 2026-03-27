--
-- AAD 2025/2026, data flow for the bit-field extract instruction
--

library IEEE;
use IEEE.std_logic_1164.all;
use IEEE.numeric_std.all;

entity bfe is
  generic
  (
    DATA_BITS_LOG2 : integer range 2 to 6 := 4                    -- use 4 by default
  );
  port
  ( 
    dst     : out std_logic_vector(2**DATA_BITS_LOG2-1 downto 0); -- 15 downto 0
    src     : in  std_logic_vector(2**DATA_BITS_LOG2-1 downto 0); -- 15 downto 0
    size    : in  std_logic_vector(   DATA_BITS_LOG2-1 downto 0); --  3 downto 0
    start   : in  std_logic_vector(   DATA_BITS_LOG2-1 downto 0); --  3 downto 0
    variant : in  std_logic                                       -- '0' for .u and '1' for .s
  );
end bfe;

architecture structural of bfe is
  -- internal signals
  constant DATA_BITS : integer := 2**DATA_BITS_LOG2;
  
  -- signal after barrel shifter (src shifted right by 'start')
  signal shifted_src : std_logic_vector(DATA_BITS-1 downto 0);
  
  -- mask signals: msfb_mask has a '1' at position 'size' (MSB of bit-field)
  --               mask has '1's for all bits outside the bit-field (to the left of MSB)
  signal msfb_mask : std_logic_vector(DATA_BITS-1 downto 0);
  signal mask      : std_logic_vector(DATA_BITS-1 downto 0);
  
  -- extracted MSB of the bit-field (for sign extension)
  signal msfb      : std_logic;
  
  -- intermediate signal for masking
  signal and_result : std_logic_vector(DATA_BITS-1 downto 0);
  
  -- fill bit for extension (0 for unsigned, msfb for signed)
  signal fill_bit : std_logic;
  
begin
  -- Step 1: Shift src right by 'start' amount so LSB of bit-field is at bit 0
  -- Use logical shift (missing='0') since we don't care about upper bits yet
  shifter : entity work.barrel_shift_right(behavioral)
    generic map
    (
      DATA_BITS_LOG2 => DATA_BITS_LOG2
    )
    port map
    (
      data_in  => src,
      data_out => shifted_src,
      shift    => start,
      missing  => '0'  -- logical shift (fill with zeros)
    );
  
  -- Step 2: Generate masks using comparators
  -- For each bit position i, compare i with size:
  --   if i == size: msfb_mask(i) = '1', mask(i) = '0'
  --   if i > size:  msfb_mask(i) = '0', mask(i) = '1'
  --   if i < size:  msfb_mask(i) = '0', mask(i) = '0'
  gen_masks : for i in 0 to DATA_BITS-1 generate
    constant i_vector : std_logic_vector(DATA_BITS_LOG2-1 downto 0) := std_logic_vector(to_unsigned(i, DATA_BITS_LOG2));
    signal eq_i : std_logic;
    signal gt_i : std_logic;
  begin
    comp_i : entity work.comparator_n(structural)
      generic map
      (
        N => DATA_BITS_LOG2
      )
      port map
      (
        a  => i_vector,
        b  => size,
        lt => open,       -- don't need less-than
        eq => eq_i,       -- equals
        gt => gt_i        -- greater-than
      );
    
    -- msfb_mask(i) = '1' when i == size
    msfb_mask(i) <= eq_i;
    
    -- mask(i) = '1' when i > size
    mask(i) <= gt_i;
  end generate;
  
  -- Step 3: Extract MSB of bit-field using bit-wise AND and reduction OR
  -- This gives us the value of the most significant bit of the bit-field
  and_result <= shifted_src and msfb_mask;
  
  -- Reduction OR (VHDL-93 compatible): manual OR of all bits
  process(and_result)
    variable temp : std_logic;
  begin
    temp := '0';
    for i in 0 to DATA_BITS-1 loop
      temp := temp or and_result(i);
    end loop;
    msfb <= temp;
  end process;
  
  -- Step 4: Determine fill bit based on variant
  -- variant='0' (.u): fill with 0 (zero extension)
  -- variant='1' (.s): fill with msfb (sign extension)
  fill_bit <= msfb and variant;
  
  -- Step 5: Generate output by masking out bits outside bit-field and filling with fill_bit
  -- For each bit position:
  --   if mask(i)='1' (outside bit-field): dst(i) = fill_bit
  --   if mask(i)='0' (inside bit-field):  dst(i) = shifted_src(i)
  gen_output : for i in 0 to DATA_BITS-1 generate
    dst(i) <= fill_bit when mask(i) = '1' else shifted_src(i);
  end generate;
  
end structural;
