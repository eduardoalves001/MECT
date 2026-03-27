%% Task 2b, c, d)

fprintf("Task2b, c, d) - Valores\n");
P = 1e5;    	% stopping criteria
N = 50;         % number of runs
alfa = 0.1;     % 90% confidence intervals
C = 10;         % C = 10 Mbps
f = 1e6;
lambda = 1500;
b = 10^-5;
n = [10 20 30 40];

PLd = zeros(1,N);
APDd = zeros(1,N);
MPDd = zeros(1,N);

PLv = zeros(1,N);
APDv = zeros(1,N);
MPDv = zeros(1,N);

TT = zeros(1,N);

PLd_values = zeros(1, length(n));
PLd_term = zeros(1, length(n));
PLv_values = zeros(1, length(n));
PLv_term = zeros(1, length(n));

APDd_values = zeros(1, length(n));
APDd_term = zeros(1, length(n));
APDv_values = zeros(1, length(n));
APDv_term = zeros(1, length(n));

TT_values = zeros(1, length(n));
TT_term = zeros(1, length(n));

for i = 1:length(n)
    for it = 1:N
       [PLd(it) , APDd(it) , MPDd(it) , PLv(it) , APDv(it) , MPDv(it), TT(it)] = Simulator3A(lambda,C,f,P, n(i), b);
    end

    fprintf('Valor de n: %d\n', n(i));
    mediaPLd = mean(PLd);
    termPLd = norminv(1-alfa/2) * sqrt(var(PLd)/N);
    fprintf('PacketLoss of data(%%)\t  = %.2e +- %.2e\n', mediaPLd, termPLd);
    
    mediaPLv = mean(PLv);
    termPLv = norminv(1-alfa/2) * sqrt(var(PLv)/N);
    fprintf('PacketLoss of VoIP (%%)\t  = %.2e +- %.2e\n', mediaPLv, termPLv);
    
    mediaAPDd = mean(APDd);
    termAPDd = norminv(1-alfa/2) * sqrt(var(APDd)/N);
    fprintf('Ag. Packet Delay of data (ms)\t = %.2e +- %.2e\n', mediaAPDd, termAPDd);
    
    mediaAPDv = mean(APDv);
    termAPDv = norminv(1-alfa/2) * sqrt(var(APDv)/N);
    fprintf('Ag. Packet Delay of VoIP (ms)\t = %.2e +- %.2e\n', mediaAPDv, termAPDv);
    
    mediaMPDd = mean(MPDd);
    termMPDd = norminv(1-alfa/2) * sqrt(var(MPDd)/N);
    fprintf('Max. Packet Delay of data (ms)\t = %.2e +- %.2e\n', mediaMPDd, termMPDd);
    
    mediaMPDv = mean(MPDv);
    termMPDv = norminv(1-alfa/2) * sqrt(var(MPDv)/N);
    fprintf('Max. Packet Delay of VoIP (ms)\t = %.2e +- %.2e\n', mediaMPDv, termMPDv);
    
    mediaTT = mean(TT);
    termTT = norminv(1-alfa/2) * sqrt(var(TT)/N);
    fprintf('Througtput (Mbps)\t = %.2e +- %.2e\n', mediaTT, termTT);

    PLd_values(i) = mediaPLd;
    PLd_term(i) = termPLd;
    PLv_values(i) = mediaPLv;
    PLv_term(i) = termPLv;

    APDd_values(i) = mediaAPDd;
    APDd_term(i) = termAPDd;
    APDv_values(i) = mediaAPDv;
    APDv_term(i) = termAPDv;

    TT_values(i) = mediaTT;
    TT_term(i) = termTT;
end

%% Task 2b) - Gráficos
figure(1);
hold on; grid on;
bar(n, PLd_values);
er=errorbar(n, PLd_values, PLd_term);
er.Color = [0 0 0];
er.LineStyle = 'none';
xlabel('Número de fluxos VoIP (n)');
ylabel('Packet Loss (%)');
xticks(n);
title('Task 2(b) - Packet Loss (Data)');
hold off;

figure(2);
hold on; grid on;
bar(n, PLv_values);
er= errorbar(n, PLv_values, PLv_term);
er.Color = [0 0 0];
er.LineStyle = 'none';
xlabel('Número de fluxos VoIP (n)');
xticks(n);
ylabel('Packet Loss (%)');
title('Task 2(b) - Packet Loss (VoIP)');
hold off;

%% Task 2c) - Gráficos
figure(1);
hold on; grid on;
bar(n, APDd_values);
er=errorbar(n, APDd_values, APDd_term);
er.Color = [0 0 0];
er.LineStyle = 'none';
xlabel('Número de fluxos VoIP (n)');
ylabel('Average Packet Delay (ms)');
xticks(n);
title('Task 2(c) - Average Packet Delay (Data)');
hold off;

figure(2);
hold on; grid on;
bar(n, APDv_values);
er= errorbar(n, APDv_values, APDv_term);
er.Color = [0 0 0];
er.LineStyle = 'none';
xlabel('Número de fluxos VoIP (n)');
ylabel('Average Packet Delay (ms)');
xticks(n);
title('Task 2(c) - Average Packet Delay (VoIP)');
hold off;

%% Task 2d) - Gráfico
figure(1);
hold on; grid on;
bar(n, TT_values);
er=errorbar(n, TT_values, TT_term);
er.Color = [0 0 0];
er.LineStyle = 'none';
xlabel('Número de fluxos VoIP (n)');
ylabel('Throughput (Mbps)');
xticks(n);
title('Task 2(d) - Throughput total do link');
hold off;

%% Task 2e)
prob_elements = (1 - (0.19 + 0.23 + 0.17)) / ((109 - 65 + 1) + (1517 - 111 + 1));

avg_packet_size_data = 0.19*64 + 0.23*110 + 0.17*1518 + sum((65:109)*(prob_elements)) + sum((111:1517)*(prob_elements));
lambda = 1500; 
data_TT = lambda * avg_packet_size_data * 8 / 10^6; 

% VoIP parameters
avg_packet_size_voip = ( 110 + 130 ) / 2;
time_voip = (16 + 24) / 2 / 10^3; 
rate_single_voip = 1 / time_voip;

voip_flows = [10, 20, 30, 40];

for i = 1:length(voip_flows)
    n_voip = voip_flows(i); 
    total_voip_rate = n_voip * rate_single_voip; 
    voip_TT = total_voip_rate * avg_packet_size_voip * 8 / 10^6;
    final_TT = data_TT + voip_TT;

    fprintf('n= %d; Throughput = %.2f Mbps\n', n_voip, final_TT);
end