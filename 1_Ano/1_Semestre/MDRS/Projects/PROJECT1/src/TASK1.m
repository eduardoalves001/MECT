%% Task 1 a)
fprintf('TASK 1a)');
P = 1e5;    	% stopping criteria
N = 50;         % number of runs
alfa = 0.1;     % 90% confidence intervals
C = 10;         % C = 10 Mbps
f = 1e6;
lambda = [1100 1300 1500 1700 1900];

PL = zeros(1, N);
APD = zeros(1, N);
MPD = zeros(1, N);
TT = zeros(1, N);

PL_values = zeros(1, length(lambda));
PL_term = zeros(1, length(lambda));
APD_values = zeros(1, length(lambda));
APD_term = zeros(1, length(lambda));

for i = 1:length(lambda)
    for it = 1:N
        [PL(it), APD(it), MPD(it), TT(it)] = Simulator1(lambda(i),C,f,P);
    end
    
    fprintf('Valor de lambda: %d\n', lambda(i));
    mediaPL = mean(PL);
    termPL = norminv(1-alfa/2) * sqrt(var(PL)/N);
    PL_values(i) = mediaPL;
    PL_term(i) = termPL;
    fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL, termPL);
    
    mediaAPD = mean(APD);
    termAPD = norminv(1-alfa/2) * sqrt(var(APD)/N);
    APD_values(i) = mediaAPD;
    APD_term(i) = termAPD;
    fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD, termAPD);
end

fprintf('\nSimulation ended!\n');



figure(1);
hold on;
grid on;
bar(lambda, PL_values);
er = errorbar(lambda, PL_values, PL_term);
er.Color = [0 0 0];                            
er.LineStyle = 'none';  
xlabel('Packet Rate (pps)');
xticks(lambda);
ylabel('Packet Loss (%)');
title ('Task 1(a) - Packet Loss vs Packet Rate');
hold off

figure(2);
hold on;
grid on;
bar(lambda, APD_values);
ylim([0 9]);
er = errorbar(lambda, APD_values, APD_term);
er.Color = [0 0 0];                            
er.LineStyle = 'none';  
xlabel('Packet Rate (pps)');
xticks(lambda);
ylabel('Average packet delay (ms)');
title('Task 1(a) - Average Packet Delay vs. Packet Rate');
hold off

%% Task 1b)
fprintf('TASK 1b)\n');
P = 1e5;        % stopping criteria
N = 50;         % number of runs
alfa = 0.1;     % 90% confidence intervals
C = 10;         % C = 10 Mbps
f = 1e4;
lambda = [1100 1300 1500 1700 1900];

PL = zeros(1, N);
APD = zeros(1, N);
MPD = zeros(1, N);
TT = zeros(1, N);

PL_values = zeros(1, length(lambda));
PL_term = zeros(1, length(lambda));
APD_values = zeros(1, length(lambda));
APD_term = zeros(1, length(lambda));

for i = 1:length(lambda)
    for it = 1:N
        [PL(it), APD(it), MPD(it), TT(it)] = Simulator1(lambda(i),C,f,P);
    end
    
    fprintf('Valor de lambda: %d\n', lambda(i));
    mediaPL = mean(PL);
    termPL = norminv(1-alfa/2) * sqrt(var(PL)/N);
    PL_values(i) = mediaPL;
    PL_term(i) = termPL;
    fprintf('Packet Loss (%%) = %.2e +- %.2e\n', mediaPL, termPL);
    
    mediaAPD = mean(APD);
    termAPD = norminv(1-alfa/2) * sqrt(var(APD)/N);
    APD_values(i) = mediaAPD;
    APD_term(i) = termAPD;
    fprintf('Average Packet Delay (ms) = %.2e +- %.2e\n', mediaAPD, termAPD);
end

fprintf('\nSimulation ended!\n');

figure(1);
hold on;
grid on;
bar(lambda, PL_values);
er = errorbar(lambda, PL_values, PL_term);
er.Color = [0 0 0];                            
er.LineStyle = 'none';  
xlabel('Packet Rate (pps)');
xticks(lambda);
ylabel('Packet Loss (%)');
title ('Task 1(b) - Packet Loss vs Packet Rate');
hold off

figure(2);
hold on;
grid on;
bar(lambda, APD_values);
ylim([0 9]);
er = errorbar(lambda, APD_values, APD_term);
er.Color = [0 0 0];                            
er.LineStyle = 'none';  
xlabel('Packet Rate (pps)');
xticks(lambda);
ylabel('Average packet delay (ms)');
title('Task 1(b) - Average Packet Delay vs. Packet Rate');
hold off

%% TASK 1d
fprintf('TASK 1d)\n');
P = 1e5;        % stopping criteria
N = 50;         % number of runs
alfa = 0.1;     % 90% confidence intervals
C = 10;         % C = 10 Mbps
f = 1e6;
lambda = 1900;

PL = zeros(1, N);
PL_64 = zeros(1, N);
PL_110 = zeros(1, N);
PL_1518 = zeros(1, N);
APD = zeros(1, N);
APD_64 = zeros(1, N);
APD_110 = zeros(1, N);
APD_1518 = zeros(1, N);
MPD = zeros(1, N);
TT = zeros(1, N);

for it = 1:N
    [PL(it) , PL_64(it), PL_110(it), PL_1518(it), APD(it) , APD_64(it), APD_110(it), APD_1518(it), MPD(it) , TT(it)] = Simulator1A(lambda,C,f,P);
end

mediaPL = mean(PL);
termPL = norminv(1-alfa/2) * sqrt(var(PL)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL, termPL);
    
mediaAPD = mean(APD);
termAPD = norminv(1-alfa/2) * sqrt(var(APD)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD, termAPD);

fprintf('Packet Size = 64 bytes\n');
mediaPL_64 = mean(PL_64);
termPL_64 = norminv(1-alfa/2) * sqrt(var(PL_64)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_64, termPL_64);
    
mediaAPD_64 = mean(APD_64);
termAPD_64 = norminv(1-alfa/2) * sqrt(var(APD_64)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_64, termAPD_64);

fprintf('Packet Size = 110 bytes\n');
mediaPL_110 = mean(PL_110);
termPL_110 = norminv(1-alfa/2) * sqrt(var(PL_110)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_110, termPL_110);
    
mediaAPD_110 = mean(APD_110);
termAPD_110 = norminv(1-alfa/2) * sqrt(var(APD_110)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_110, termAPD_110);

fprintf('Packet Size = 1518 bytes\n');
mediaPL_1518 = mean(PL_1518);
termPL_1518 = norminv(1-alfa/2) * sqrt(var(PL_1518)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_1518, termPL_1518);
    
mediaAPD_1518 = mean(APD_1518);
termAPD_1518 = norminv(1-alfa/2) * sqrt(var(APD_1518)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_1518, termAPD_1518);


fprintf('\nSimulation ended!\n');


PL_means  = [mediaPL,  mediaPL_64,  mediaPL_110,  mediaPL_1518];
PL_terms  = [termPL,   termPL_64,   termPL_110,   termPL_1518];
APD_means = [mediaAPD, mediaAPD_64, mediaAPD_110, mediaAPD_1518];
APD_terms = [termAPD,  termAPD_64,  termAPD_110,  termAPD_1518];

figure(1); 
hold on; 
grid on;
bar(PL_means);
er = errorbar(1:4, PL_means, PL_terms);
er.Color = [0 0 0];
er.LineStyle = 'none';
xticks([1 2 3 4])
xticklabels({'All','64 Bytes','110 Bytes','1518 Bytes'})
xlabel('Packet Size')
ylabel('Packet loss (%)');
title('Task 1(d) – Packet Loss per Packet Size')

hold off;

figure(2);
hold on; 
grid on;
bar(APD_means);
ylim([0 9]);
er = errorbar(1:4, APD_means, APD_terms);
er.Color = [0 0 0];                            
er.LineStyle = 'none'; 
ylabel('Average packet delay (ms)');
xticks([1 2 3 4])
xticklabels({'All','64 Bytes','110 Bytes','1518 Bytes'})
xlabel('Packet Size')
title('Task 1(d) – Average Packet Delay per Packet Size');
hold off;

%% Task 1e - Teórica - não sei como fazer ainda, mas deve ser adaptar este código!
P = 1e5;
N = 50;
C = 10;
f = 1e6;
lambda = 1900;
alfa = 0.1;

x = 64:1518;
prob_elements = (1 - 0.19 - 0.23 - 0.17) / ((109-65+1) + (1517-111+1));

avg_packet_size = 0.19*64 + 0.23*110 + 0.17*1518 + sum(65:109)* prob_elements + sum(111:1517)*prob_elements;
avg_time = (avg_packet_size * 8) / (C * 10^6);

S = x .*8 / (C * 10^6);
S2 = S.^2;

for i = 1:length(x)
    if i == 1
       p = 0.19;
    elseif i == 110-64+1
       p = 0.23;
    elseif i == 1518-64+1
        p = 0.17;
    else
       p = prob_elements;
    end
    S(i) = S(i) * p;
    S2(i) = S2(i) * p;
end

ES = sum(S);
ES2 = sum(S2);

W = (lambda * ES2) / (2*(1 - lambda*ES)) + ES;
Wq = (lambda * ES2) / (2*(1 - lambda*ES));

S64 = (64 * 8) / (C * 1e6);
S110 = (110 * 8) / (C * 1e6);
S1518 = (1518 * 8) / (C * 1e6);

W64 = Wq + S64;
W110 = Wq + S110;
W1518 = Wq + S1518;

TT = lambda * avg_packet_size * 8 / 10^6;

% Lista de espera infinita - não tem packet loss.
fprintf('PL_all (%%)      = 0.0000\n');
fprintf('PL_64B (%%)      = 0.0000\n');
fprintf('PL_110B (%%)     = 0.0000\n');
fprintf('PL_1518B (%%)    = 0.0000\n');
fprintf('Delay all (ms)   = %.4f\n', W*1000);
fprintf('Delay 64B (ms)   = %.4f\n', W64*1000);
fprintf('Delay 110B (ms)  = %.4f\n', W110*1000);
fprintf('Delay 1518B (ms) = %.4f\n', W1518*1000);
fprintf('Throughput (Mb/s)= %.4f\n', TT);


%% Task 1f
fprintf('TASK 1f)\n');
P = 1e5;        % stopping criteria
N = 50;         % number of runs
alfa = 0.1;     % 90% confidence intervals
C = 10;         % C = 10 Mbps
f = 1e4;
lambda = 1900;

PL = zeros(1, N);
PL_64 = zeros(1, N);
PL_110 = zeros(1, N);
PL_1518 = zeros(1, N);
APD = zeros(1, N);
APD_64 = zeros(1, N);
APD_110 = zeros(1, N);
APD_1518 = zeros(1, N);
MPD = zeros(1, N);
TT = zeros(1, N);

for it = 1:N
    [PL(it) , PL_64(it), PL_110(it), PL_1518(it), APD(it) , APD_64(it), APD_110(it), APD_1518(it), MPD(it) , TT(it)] = Simulator1A(lambda,C,f,P);
end


mediaPL = mean(PL);
termPL = norminv(1-alfa/2) * sqrt(var(PL)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL, termPL);
    
mediaAPD = mean(APD);
termAPD = norminv(1-alfa/2) * sqrt(var(APD)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD, termAPD);

fprintf('Packet Size = 64 bytes\n');
mediaPL_64 = mean(PL_64);
termPL_64 = norminv(1-alfa/2) * sqrt(var(PL_64)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_64, termPL_64);
    
mediaAPD_64 = mean(APD_64);
termAPD_64 = norminv(1-alfa/2) * sqrt(var(APD_64)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_64, termAPD_64);

fprintf('Packet Size = 110 bytes\n');
mediaPL_110 = mean(PL_110);
termPL_110 = norminv(1-alfa/2) * sqrt(var(PL_110)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_110, termPL_110);
    
mediaAPD_110 = mean(APD_110);
termAPD_110 = norminv(1-alfa/2) * sqrt(var(APD_110)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_110, termAPD_110);

fprintf('Packet Size = 1518 bytes\n');
mediaPL_1518 = mean(PL_1518);
termPL_1518 = norminv(1-alfa/2) * sqrt(var(PL_1518)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_1518, termPL_1518);
    
mediaAPD_1518 = mean(APD_1518);
termAPD_1518 = norminv(1-alfa/2) * sqrt(var(APD_1518)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_1518, termAPD_1518);


fprintf('\nSimulation ended!\n');


PL_means  = [mediaPL,  mediaPL_64,  mediaPL_110,  mediaPL_1518];
PL_terms  = [termPL,   termPL_64,   termPL_110,   termPL_1518];
APD_means = [mediaAPD, mediaAPD_64, mediaAPD_110, mediaAPD_1518];
APD_terms = [termAPD,  termAPD_64,  termAPD_110,  termAPD_1518];



figure(1);
hold on; 
grid on;
bar(PL_means);
er = errorbar(1:4, PL_means, PL_terms);
er.Color = [0 0 0];                            
er.LineStyle = 'none';
ylabel('Packet loss (%)');
xticks([1 2 3 4])
xticklabels({'All','64 Bytes','110 Bytes','1518 Bytes'})
xlabel('Packet Size')
title('Task 1(f) – Packet Loss per Packet Size');
hold off;

figure(2);
hold on; 
grid on;
bar(APD_means);
er = errorbar(1:4, APD_means, APD_terms);
er.Color = [0 0 0];                            
er.LineStyle = 'none';
ylabel('Average packet delay (ms)');
xticks([1 2 3 4])
xticklabels({'All','64 Bytes','110 Bytes','1518 Bytes'})
xlabel('Packet Size')
title('Task 1(f) – Average Packet Delay per Packet Size');
hold off;


%% Task 1h
fprintf('TASK 1h)\n');
P = 1e5;     %stopping criteria
N = 50;         % number of runs
alfa = 0.1;     % 90% confidence intervals
C = 10;         % C = 10 Mbps
f = 1e6;
lambda = 1900;

PL = zeros(1, N);
PL_64 = zeros(1, N);
PL_110 = zeros(1, N);
PL_1518 = zeros(1, N);
APD = zeros(1, N);
APD_64 = zeros(1, N);
APD_110 = zeros(1, N);
APD_1518 = zeros(1, N);
MPD = zeros(1, N);
TT = zeros(1, N);

for it = 1:N
    [PL(it) , PL_64(it), PL_110(it), PL_1518(it), APD(it) , APD_64(it), APD_110(it), APD_1518(it), MPD(it) , TT(it)] = Simulator1B(lambda,C,f,P);
end

mediaPL = mean(PL);
termPL = norminv(1-alfa/2) * sqrt(var(PL)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL, termPL);
    
mediaAPD = mean(APD);
termAPD = norminv(1-alfa/2) * sqrt(var(APD)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD, termAPD);

fprintf('Packet Size = 64 bytes\n');
mediaPL_64 = mean(PL_64);
termPL_64 = norminv(1-alfa/2) * sqrt(var(PL_64)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_64, termPL_64);
    
mediaAPD_64 = mean(APD_64);
termAPD_64 = norminv(1-alfa/2) * sqrt(var(APD_64)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_64, termAPD_64);

fprintf('Packet Size = 110 bytes\n');
mediaPL_110 = mean(PL_110);
termPL_110 = norminv(1-alfa/2) * sqrt(var(PL_110)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_110, termPL_110);
    
mediaAPD_110 = mean(APD_110);
termAPD_110 = norminv(1-alfa/2) * sqrt(var(APD_110)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_110, termAPD_110);

fprintf('Packet Size = 1518 bytes\n');
mediaPL_1518 = mean(PL_1518);
termPL_1518 = norminv(1-alfa/2) * sqrt(var(PL_1518)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_1518, termPL_1518);
    
mediaAPD_1518 = mean(APD_1518);
termAPD_1518 = norminv(1-alfa/2) * sqrt(var(APD_1518)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_1518, termAPD_1518);


fprintf('\nSimulation ended!\n');


PL_means  = [mediaPL,  mediaPL_64,  mediaPL_110,  mediaPL_1518];
PL_terms  = [termPL,   termPL_64,   termPL_110,   termPL_1518];
APD_means = [mediaAPD, mediaAPD_64, mediaAPD_110, mediaAPD_1518];
APD_terms = [termAPD,  termAPD_64,  termAPD_110,  termAPD_1518];

figure(1);
hold on
grid on;
bar(PL_means);
er = errorbar(1:4, PL_means, PL_terms);
er.Color = [0 0 0];                            
er.LineStyle = 'none';
ylabel('Packet loss (%)');
title('Task 1(h) – Packet Loss per Packet Size (Priority Queuing)')
xlabel('Packet Size');
xticks(1:4);
xticklabels({'All','64 Bytes','110 Bytes','1518 Bytes'});
hold off;

figure(2); 
hold on; 
grid on;
bar(APD_means);
er = errorbar(1:4, APD_means, APD_terms);
er.Color = [0 0 0];                            
er.LineStyle = 'none';
ylabel('Average packet delay (ms)');
title('Task 1(h) – Average Packet Delay per Packet Size (Priority Queuing)')
xlabel('Packet Size');
xticks(1:4);
xticklabels({'All','64 Bytes','110 Bytes','1518 Bytes'});
hold off;

%% Task 1i
fprintf('TASK 1i)\n');
P = 1e5;     %stopping criteria
N = 50;         % number of runs
alfa = 0.1;     % 90% confidence intervals
C = 10;         % C = 10 Mbps
f = 1e4;
lambda = 1900;

PL = zeros(1, N);
PL_64 = zeros(1, N);
PL_110 = zeros(1, N);
PL_1518 = zeros(1, N);
APD = zeros(1, N);
APD_64 = zeros(1, N);
APD_110 = zeros(1, N);
APD_1518 = zeros(1, N);
MPD = zeros(1, N);
TT = zeros(1, N);

for it = 1:N
    [PL(it) , PL_64(it), PL_110(it), PL_1518(it), APD(it) , APD_64(it), APD_110(it), APD_1518(it), MPD(it) , TT(it)] = Simulator1B(lambda,C,f,P);
end

mediaPL = mean(PL);
termPL = norminv(1-alfa/2) * sqrt(var(PL)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL, termPL);
    
mediaAPD = mean(APD);
termAPD = norminv(1-alfa/2) * sqrt(var(APD)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD, termAPD);

fprintf('Packet Size = 64 bytes\n');
mediaPL_64 = mean(PL_64);
termPL_64 = norminv(1-alfa/2) * sqrt(var(PL_64)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_64, termPL_64);
    
mediaAPD_64 = mean(APD_64);
termAPD_64 = norminv(1-alfa/2) * sqrt(var(APD_64)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_64, termAPD_64);

fprintf('Packet Size = 110 bytes\n');
mediaPL_110 = mean(PL_110);
termPL_110 = norminv(1-alfa/2) * sqrt(var(PL_110)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_110, termPL_110);
    
mediaAPD_110 = mean(APD_110);
termAPD_110 = norminv(1-alfa/2) * sqrt(var(APD_110)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_110, termAPD_110);

fprintf('Packet Size = 1518 bytes\n');
mediaPL_1518 = mean(PL_1518);
termPL_1518 = norminv(1-alfa/2) * sqrt(var(PL_1518)/N);
fprintf('Packet Loss (%%)\t  = %.2e +- %.2e\n', mediaPL_1518, termPL_1518);
    
mediaAPD_1518 = mean(APD_1518);
termAPD_1518 = norminv(1-alfa/2) * sqrt(var(APD_1518)/N);
fprintf('Average Packet Delay (ms)\t = %.2e +- %.2e\n', mediaAPD_1518, termAPD_1518);


fprintf('\nSimulation ended!\n');


PL_means  = [mediaPL,  mediaPL_64,  mediaPL_110,  mediaPL_1518];
PL_terms  = [termPL,   termPL_64,   termPL_110,   termPL_1518];
APD_means = [mediaAPD, mediaAPD_64, mediaAPD_110, mediaAPD_1518];
APD_terms = [termAPD,  termAPD_64,  termAPD_110,  termAPD_1518];

% Figure 1 — Average Packet Loss (%)
figure(1);
hold on; 
grid on;
bar(PL_means);
er = errorbar(1:4, PL_means, PL_terms);
er.Color = [0 0 0];                            
er.LineStyle = 'none';
ylabel('Packet loss (%)');
title('Task 1(i) – Packet Loss per Packet Size (Priority Queuing)')
xlabel('Packet Size');
xticks(1:4);
xticklabels({'All','64 Bytes','110 Bytes','1518 Bytes'});
hold off;

figure(2); 
hold on; 
grid on;
bar(APD_means);
er = errorbar(1:4, APD_means, APD_terms);
er.Color = [0 0 0];                            
er.LineStyle = 'none';
ylabel('Average packet delay (ms)');
title('Task 1(i) – Average Packet Delay per Packet Size (Priority Queuing)')
xlabel('Packet Size');
xticks(1:4);
xticklabels({'All','64 Bytes','110 Bytes','1518 Bytes'});
hold off;