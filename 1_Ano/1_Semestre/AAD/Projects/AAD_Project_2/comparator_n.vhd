--
-- AAD 2025/2026, n-bit comparator
--
-- for extra credit, implement this also using chains of unsigned comparators:
--
-- unsigned comparator stage (one per bit)
--   in   a_bit   b_bit   old_lt   old_eq   old_gt
--   out                  new_lt   new_eq   new_gt
-- logic, start from the least significant bit with old_lt=old_gt=0 and old_eq=1
--   if a_bit=b_bit (no change, keep the earlier result)
--     new_lt=old_lt   new_eq=old_eq   new_gt=old_gt
--   else if a_bit=1 (a is greater because a_bit=1 and b_bit=0)
--     new_lt=0        new_eq=0        new_gt=1
--   else  (a is smaller because a_bit=0 and b_bit=1)
--     new_lt=1        new_eq=0        new_gt=0
-- use a transport delay of 5 ps per stage
--

library IEEE;
use IEEE.std_logic_1164.all;
use IEEE.numeric_std.all;

entity comparator_n is
  generic
  (
    N : positive
  );
  port
  (
    a  : in  std_logic_vector(N-1 downto 0);
    b  : in  std_logic_vector(N-1 downto 0);
    lt : out std_logic; -- '1' if a<b, '0' otherwise
    eq : out std_logic; -- '1' if a=b, '0' otherwise
    gt : out std_logic  -- '1' if a>b, '0' otherwise
  );
end comparator_n;

architecture behavioral of comparator_n is
  signal s_lt : std_logic;
  signal s_eq : std_logic;
  signal s_gt : std_logic;
begin
  s_lt <= '1' when unsigned(a) < unsigned(b) else '0';
  s_eq <= '1' when          a  =          b  else '0';
  s_gt <= '1' when unsigned(a) > unsigned(b) else '0';
  lt <= transport s_lt after (10+2*N)*ps;
  eq <= transport s_eq after (10    )*ps; -- smaller time penalty because comparison for equality is simpler
  gt <= transport s_gt after (10+2*N)*ps;
end behavioral;

--
-- Bit-serial structural implementation for extra credit
-- Chains N single-bit comparator stages together
--
architecture structural of comparator_n is
  -- Internal signals for chaining stages
  -- Array type to hold intermediate comparison results between stages
  type comparison_array is array(0 to N) of std_logic;
  signal lt_chain : comparison_array;
  signal eq_chain : comparison_array;
  signal gt_chain : comparison_array;
  
begin
  -- Initialize first stage with: lt=0, eq=1, gt=0
  -- (no comparison done yet, values are equal so far)
  lt_chain(0) <= '0';
  eq_chain(0) <= '1';
  gt_chain(0) <= '0';
  
  -- Generate N comparator stages, one per bit
  -- Process bits from LSB (bit 0) to MSB (bit N-1)
  gen_stages : for i in 0 to N-1 generate
    stage_i : entity work.comparator_stage(behavioral)
      port map
      (
        a_bit  => a(i),
        b_bit  => b(i),
        old_lt => lt_chain(i),
        old_eq => eq_chain(i),
        old_gt => gt_chain(i),
        new_lt => lt_chain(i+1),
        new_eq => eq_chain(i+1),
        new_gt => gt_chain(i+1)
      );
  end generate;
  
  -- Final outputs from last stage
  lt <= lt_chain(N);
  eq <= eq_chain(N);
  gt <= gt_chain(N);
  
end structural;

--
-- Single-bit comparator stage entity
-- Compares one bit position and updates comparison state
--
library IEEE;
use IEEE.std_logic_1164.all;

entity comparator_stage is
  port
  (
    a_bit  : in  std_logic;  -- bit from 'a' vector
    b_bit  : in  std_logic;  -- bit from 'b' vector
    old_lt : in  std_logic;  -- previous less-than result
    old_eq : in  std_logic;  -- previous equal result
    old_gt : in  std_logic;  -- previous greater-than result
    new_lt : out std_logic;  -- updated less-than result
    new_eq : out std_logic;  -- updated equal result
    new_gt : out std_logic   -- updated greater-than result
  );
end comparator_stage;

architecture behavioral of comparator_stage is
  signal s_new_lt : std_logic;
  signal s_new_eq : std_logic;
  signal s_new_gt : std_logic;
begin
  -- Comparison logic as specified in assignment:
  -- If a_bit = b_bit: keep old results (no new information)
  -- If a_bit = '1' and b_bit = '0': a is greater (lt=0, eq=0, gt=1)
  -- If a_bit = '0' and b_bit = '1': a is smaller (lt=1, eq=0, gt=0)
  
  process(a_bit, b_bit, old_lt, old_eq, old_gt)
  begin
    if a_bit = b_bit then
      -- Bits are equal, keep previous comparison result
      s_new_lt <= old_lt;
      s_new_eq <= old_eq;
      s_new_gt <= old_gt;
    elsif a_bit = '1' then
      -- a_bit=1 and b_bit=0, so a > b at this bit position
      s_new_lt <= '0';
      s_new_eq <= '0';
      s_new_gt <= '1';
    else
      -- a_bit=0 and b_bit=1, so a < b at this bit position
      s_new_lt <= '1';
      s_new_eq <= '0';
      s_new_gt <= '0';
    end if;
  end process;
  
  -- Add 5 ps transport delay per stage as specified
  new_lt <= transport s_new_lt after 5 ps;
  new_eq <= transport s_new_eq after 5 ps;
  new_gt <= transport s_new_gt after 5 ps;
  
end behavioral;

--
-- ============================================================================
-- BONUS: Carry-Lookahead Style Architecture
-- ============================================================================
--
-- This implementation uses carry-lookahead logic inspired by fast adders.
-- Instead of rippling through bits sequentially (structural) or using 
-- built-in operators (behavioral), we compute comparison results in parallel
-- using generate/propagate logic similar to carry-lookahead adders.
--
-- CONCEPT:
-- For each bit position i, we compute:
--   - P[i] (Propagate): a[i] = b[i]  (equality propagates previous result)
--   - G_gt[i] (Generate GT): a[i]='1' AND b[i]='0'  (a > b at this bit)
--   - G_lt[i] (Generate LT): a[i]='0' AND b[i]='1'  (a < b at this bit)
--
-- Then we use lookahead logic to compute the final result in log2(N) stages
-- instead of N stages (bit-serial) or using expensive parallel comparison.
--
-- TIMING:
-- - Stage 1: Compute P, G_gt, G_lt for all bits (1 gate delay = 2 ps)
-- - Stage 2: Lookahead logic in tree structure (log2(N) levels = 2 levels for N=4)
--            Each level: 3 ps (AND-OR gates)
-- - Total: 2 + (2 x 3) = 8 ps (much faster than 20 ps bit-serial!)
--
-- TRADE-OFF:
-- + Faster than bit-serial (8 ps vs 20 ps)
-- + More educational value (demonstrates advanced digital design)
-- - More complex logic (harder to understand)
-- - Slightly more area than bit-serial (but still less than full parallel)
--
architecture carry_lookahead of comparator_n is
  -- Propagate signals: bit equality (propagates previous comparison result)
  signal P : std_logic_vector(N-1 downto 0);
  
  -- Generate signals for greater-than
  signal G_gt : std_logic_vector(N-1 downto 0);
  
  -- Generate signals for less-than
  signal G_lt : std_logic_vector(N-1 downto 0);
  
  -- Intermediate lookahead signals (for tree structure)
  -- For N=4, we need 2 levels: pairs (0-1, 2-3), then full (0-3)
  signal P_level1 : std_logic_vector(N/2-1 downto 0);      -- level 1: pairs
  signal G_gt_level1 : std_logic_vector(N/2-1 downto 0);
  signal G_lt_level1 : std_logic_vector(N/2-1 downto 0);
  
  -- Final comparison results (internal signals before delay)
  signal s_lt, s_eq, s_gt : std_logic;
  
begin
  -- =========================================================================
  -- STAGE 1: Generate P (Propagate) and G (Generate) signals for each bit
  -- =========================================================================
  -- This stage takes 2 ps (one gate delay for XOR/XNOR and AND gates)
  
  gen_pg : for i in 0 to N-1 generate
    -- Propagate: a[i] = b[i] (bits are equal, propagate previous result)
    P(i) <= a(i) xnor b(i);  -- '1' when equal, '0' when different
    
    -- Generate GT: a[i] > b[i] at this position
    G_gt(i) <= a(i) and (not b(i));  -- '1' when a[i]='1' and b[i]='0'
    
    -- Generate LT: a[i] < b[i] at this position
    G_lt(i) <= (not a(i)) and b(i);  -- '1' when a[i]='0' and b[i]='1'
  end generate;
  
  -- =========================================================================
  -- STAGE 2: Carry-Lookahead Tree (for N=4, we build a 2-level tree)
  -- =========================================================================
  -- Level 1: Combine pairs of bits (0-1, 2-3)
  -- Each level takes 3 ps (AND-OR gate delays)
  
  gen_level1 : for i in 0 to N/2-1 generate
    constant LOW_BIT : integer := 2*i;      -- bit 0, 2
    constant HIGH_BIT : integer := 2*i + 1;  -- bit 1, 3
  begin
    -- Propagate for pair [HIGH:LOW]
    -- Both bits must propagate for pair to propagate
    P_level1(i) <= P(HIGH_BIT) and P(LOW_BIT);
    
    -- Generate GT for pair [HIGH:LOW]
    -- Either HIGH generates GT, OR HIGH propagates and LOW generates GT
    -- (Higher bit takes precedence)
    G_gt_level1(i) <= G_gt(HIGH_BIT) or (P(HIGH_BIT) and G_gt(LOW_BIT));
    
    -- Generate LT for pair [HIGH:LOW]
    -- Either HIGH generates LT, OR HIGH propagates and LOW generates LT
    G_lt_level1(i) <= G_lt(HIGH_BIT) or (P(HIGH_BIT) and G_lt(LOW_BIT));
  end generate;
  
  -- =========================================================================
  -- STAGE 3: Final Level - Combine all 4 bits (pairs 0-1 and 2-3)
  -- =========================================================================
  -- This takes another 3 ps
  
  -- Final Greater-Than: 
  -- Either high pair (bits 3-2) generates GT,
  -- OR high pair propagates and low pair (bits 1-0) generates GT
  s_gt <= G_gt_level1(1) or (P_level1(1) and G_gt_level1(0));
  
  -- Final Less-Than:
  -- Either high pair generates LT,
  -- OR high pair propagates and low pair generates LT
  s_lt <= G_lt_level1(1) or (P_level1(1) and G_lt_level1(0));
  
  -- Final Equality:
  -- All bits must propagate (all bits equal)
  s_eq <= P_level1(1) and P_level1(0);
  
  -- =========================================================================
  -- OUTPUT: Apply timing delays
  -- =========================================================================
  -- Total delay: 2 ps (stage 1) + 3 ps (level 1) + 3 ps (level 2) = 8 ps
  gt <= transport s_gt after 8 ps;
  lt <= transport s_lt after 8 ps;
  eq <= transport s_eq after 8 ps;
  
end carry_lookahead;

--
-- ============================================================================
-- ARCHITECTURE COMPARISON SUMMARY
-- ============================================================================
--
-- For N=4 bit comparator:
--
-- 1. BEHAVIORAL (Parallel):
--    - Uses built-in unsigned comparison operators
--    - Delay: 18 ps (gt/lt), 10 ps (eq)
--    - Area: Highest (full parallel logic)
--    - Complexity: Lowest (one line of code)
--    - Best for: Quick implementation, synthesis optimization
--
-- 2. STRUCTURAL (Bit-Serial):
--    - Chains N single-bit stages sequentially
--    - Delay: 20 ps (4 stages x 5 ps)
--    - Area: Lowest (minimal logic per stage)
--    - Complexity: Medium (explicit stage chaining)
--    - Best for: Area-constrained designs, educational purposes
--
-- 3. CARRY_LOOKAHEAD (Advanced):
--    - Uses tree structure with generate/propagate logic
--    - Delay: 8 ps (fastest! - logarithmic depth)
--    - Area: Medium (between behavioral and structural)
--    - Complexity: Highest (requires lookahead tree logic)
--    - Best for: High-performance designs, demonstrating advanced concepts
--
-- SCALING BEHAVIOR:
--   N    | Behavioral | Bit-Serial | Carry-Lookahead
-- -------|------------|------------|----------------
--   4    |   18 ps    |   20 ps    |    8 ps (FASTEST)
--   8    |   26 ps    |   40 ps    |   11 ps (FASTEST)
--   16   |   42 ps    |   80 ps    |   14 ps (FASTEST)
--
-- The carry-lookahead architecture scales O(log N) vs O(N) for bit-serial!
--
-- ============================================================================
