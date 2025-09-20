% Consider a wireless link between multiple stations for data communications with a bit error rate
% (ber) of 𝑝. Assume that transmission errors in the different bits of a data frame are statistically
% independent (i.e., the number of errors of a data packet is a binomial random variable).

% 2a) Determine the probability of a data frame of 100 Bytes to be received without errors when
% 𝑝 = 10−2. Answer: 0.0322%

bytes = 100*8;
bits = 8;
p = 1e-2;

noerrors = nchoosek(bytes*bits, 0);
pnoerrors = noerrors * (1 - p)^(bytes*bits);

fprintf('Answer of 2a: %.4f%%\n', pnoerrors*100)

% 2b) Determine the probability of a data frame of 1000 Bytes to be received with exactly one
% error when 𝑝 = 10−3. Answer: 0.2676%

n = 1000*8;    
p = 1e-3;     
k = 1;     

P = nchoosek(n,k) * p^k * (1-p)^(n-k);

fprintf('Answer of 2b: %.4f%%\n', P*100)

% 2c) Determine the probability of a data frame of 200 Bytes to be received with one or more
% errors when 𝑝 = 10−4. Answer: 14.7863%

% p1oumais = 1- pnoerrors;

p = 1e-4;
bytes = 200;
bits = 8;

noerrors = nchoosek(bytes*bits, 0);
pnoerrors = noerrors * (1 - p)^(bytes*bits);
moreThanOneError = 1 - pnoerrors;

fprintf('Answer of 2c: %.4f%%\n', moreThanOneError*100)

% 2d) Draw a plot using a logarithmic scale for the X-axis (use the MATLAB function
% semilogx) with the same look as the plot below with the probability of a data frame (of
% size 100 Bytes, 200 Bytes or 1000 Bytes) being received without errors as a function of
% the ber (from 𝑝 = 10−8 up to 𝑝 = 10−2). What do you conclude from these results?

figure(1)

x = logspace(-8, -2);
bytes = 100;
no_err100 = nchoosek(bytes*bits, 0) .* (1-x).^(bytes*bits);
    
bytes = 200;
no_err200 = nchoosek(bytes*bits, 0) .* (1-x).^(bytes*bits);

bytes = 1000;
no_err1000 = nchoosek(bytes*bits, 0) .* (1-x).^(bytes*bits);

semilogx(x, 100*no_err100, 'b', x, 100*no_err200, 'b--', x, 100*no_err1000, 'b:');
xlabel('Bit Error Rate');
legend('100 Bytes','200 Bytes','1000 Bytes', 'location','southwest');
title('Probability of packet reception without errors (%)');
grid on;

% 2e) Draw a plot using a logarithmic scale for the Y-axis (use the MATLAB function
% semilogy) with the same look as the plot below with the probability of a data frame
% being received without errors (for 𝑝 = 10−4, 10−3 and 10−2) as a function of the packet
% size (all integer values from 64 Bytes up to 1518 Bytes). What do you conclude from
% these results?

figure(2)
bits = 8;
x = linspace(64* bits, 1518*bits);

no_errors4 = (1 - 1e-4).^x;
no_errors3 = (1 - 1e-3).^x;
no_errors2 = (1 - 1e-2).^x;
    
semilogy(x, no_errors4, 'b', x, no_errors3, 'b--', x, no_errors2, 'b:');
xlabel('Packet size (Bytes)');
legend('ber=1e-4','ber=1e-3','ber=1e-3', 'location','southwest');
title('Probability of packet reception without errors (%)');
grid on
