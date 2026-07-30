/**
 * 資格管理ページ
 */

import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { qualificationApi } from '../features/master/masterApi';
import type {
    Qualification,
    CreateQualificationRequest,
    UpdateQualificationRequest,
} from '../features/master/types';
import { ROUTES } from '../constants/routes';
import { MasterSelector } from '../components/masterPage/MasterSelector';
import { MasterTable } from '../components/masterPage/MasterTable';
import { MasterButtonArea } from '../components/masterPage/MasterButonArea';
import {
    canCreate,
    canDeleteHistory,
    canUpdate,
    isActiveHistory,
    sortMasterHistory,
} from '../components/masterPage/MasterUtils';
import { MASTER_MESSAGES } from '../features/master/messages';

export default function QualificationPage() {

    const navigate = useNavigate();
    const [qualifications, setQualifications] = useState<Qualification[]>([]);
    const [keyword, setKeyword] = useState('');
    const [selected, setSelected] = useState<Qualification | null>(null);
    const [qualificationName, setQualificationName] = useState('');
    const [qualificationAllowance, setQualificationAllowance]
        = useState('');
    const [isAdvance, setIsAdvance] = useState(false);
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');
    const [message,setMessage] = useState('');
    const [isCreateMode, setIsCreateMode] = useState(false);

    useEffect(() => {
        loadQualifications();
    }, []);

    const loadQualifications = async () => {
        try {
            const data = await qualificationApi.findAll();
            setQualifications(data);

        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.dataFetchFailed
            );
        }
    };

    const filteredQualifications =
        useMemo(() => {
            const filtered =
                !keyword.trim()
                    ? qualifications
                    : qualifications.filter(
                        qualification =>
                            qualification.qualificationName.includes(
                                keyword,
                            ),
                    );

            return sortMasterHistory(
                filtered,
                row => row.qualificationId,
            );

        }, [qualifications, keyword]);

    const handleSelect = (
        qualification: Qualification,
    ) => {
        setSelected(qualification);
        setIsCreateMode(false);
        setQualificationName(
            qualification.qualificationName);
        setQualificationAllowance(
            String(qualification.qualificationAllowance));
        setIsAdvance(qualification.isAdvance);
        setStartDate(qualification.startDate);
        setEndDate(qualification.endDate ?? '');
        setMessage('');
    };

    const handleNew = () => {
        setSelected(null);
        setIsCreateMode(true);
        setQualificationName('');
        setQualificationAllowance('');
        setIsAdvance(false);
        setStartDate('');
        setEndDate('');
    };

    const handleCreate = async () => {
        try {
            const request:
                CreateQualificationRequest = {
                qualificationName,
                qualificationAllowance:
                    Number(qualificationAllowance),
                isAdvance,
                startDate
            };
            await qualificationApi
                .createQualification(request);
            await loadQualifications();
            setMessage(MASTER_MESSAGES.createSuccess);
            handleNew();

        } catch (error) {

            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.createFailed,
            );
        }
    };

    const handleUpdate = async () => {
        if (!selected) {
            return;
        }
        try {
            const request:
                UpdateQualificationRequest = {
                qualificationId:
                    selected.qualificationId,
                qualificationName: 
                    qualificationName,
                qualificationAllowance:
                    Number(qualificationAllowance),
                isAdvance,
                startDate
            };

            await qualificationApi.updateQualification(request);
            await loadQualifications();
            setMessage(MASTER_MESSAGES.updateSuccess);

        } catch (error) {
            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.updateFailed);
        }
    };

    const handleDelete = async () => {
        if (!selected) {
            return;
        }
        if (
            !window.confirm(
                MASTER_MESSAGES.deleteConfirm,
            )
        ) {
            return;
        }
        try {
            await qualificationApi
                .deleteQualification(
                    selected.qualificationId,
                    selected.startDate,
                );

            await loadQualifications();
            handleNew();
            setMessage(
                MASTER_MESSAGES.deleteSuccess,
            );
        } catch (error) {

            setMessage(
                error instanceof Error
                    ? error.message
                    : MASTER_MESSAGES.deleteFailed,
            );
        }
    };

    const handleClear = () => {
        setKeyword('');
        setQualificationName('');
        setQualificationAllowance('');
        setIsAdvance(false);
        setStartDate('');
        setEndDate('');
        setSelected(null);
        setIsCreateMode(false);
        setMessage('');
    };

    return (
        <div className="department-page">
            <div className="page-header">
                <h1>
                    マスタデータ管理画面
                </h1>
            </div>

            <div className="search-area">

                <div className="search-item">
                    <label>
                        管理対象マスタ
                    </label>

                    <MasterSelector
                        value="qualification"
                    />
                </div>

                <div className="search-item">
                    <label>
                        検索条件
                    </label>
                    <input
                        value={keyword}
                        placeholder="名称で検索"
                        onChange={e =>
                            setKeyword(
                                e.target.value)
                        }
                    />

                </div>

                <button onClick={handleNew}>
                    新規作成
                </button>
            </div>

            <div className="content-area">
                <div className="master-panel">
                    <div className="panel-header">
                        マスター一覧
                    </div>

                    <div className="panel-body">
                        <MasterTable
                            rows={filteredQualifications}
                            onSelect={handleSelect}
                            columns={[
                                {
                                    header: 'ID',
                                    render: row =>
                                        row.qualificationId,
                                },
                                {
                                    header: '資格名',
                                    render: row =>
                                        row.qualificationName,
                                },
                                {
                                    header: '手当額',
                                    render: row =>
                                        row.qualificationAllowance,
                                },
                                {
                                    header: '高度資格',
                                    render: row =>
                                        row.isAdvance
                                            ? '○'
                                            : '',
                                },
                                {
                                    header: '適用期間',
                                    render: row =>
                                        `${row.startDate} ～ ${row.endDate ?? ''}`,
                                },
                                {
                                    header: '状態',
                                    render: row =>
                                        isActiveHistory(
                                            row.startDate,
                                            row.endDate,
                                        )
                                            ? '有効'
                                            : '無効',
                                },
                            ]}
                        />

                    </div>

                </div>

                <div className="master-panel">
                    <div className="panel-header">
                        詳細入力エリア
                    </div>

                    <div className="panel-body">
                        <div className="form-row">
                            <label>
                                資格名
                            </label>
                            <input
                                value={qualificationName}
                                onChange={e =>
                                    setQualificationName(
                                        e.target.value,
                                    )
                                }
                            />

                        </div>

                        <div className="form-row">
                            <label>
                                手当額
                            </label>
                            <input
                                type="number"
                                min="0"
                                value={
                                    qualificationAllowance
                                }
                                onChange={e =>
                                    setQualificationAllowance(
                                        e.target.value
                                    )
                                }
                            />

                        </div>
                        <div className="form-row">
                            <label>
                                高度資格
                            </label>

                            <input
                                type="checkbox"
                                checked={isAdvance}
                                onChange={e =>
                                    setIsAdvance(
                                        e.target.checked,
                                    )
                                }
                            />

                        </div>

                        <div className="form-row">
                            <label>
                                適用開始日
                            </label>
                            <input
                                type="date"
                                value={startDate}
                                onChange={e =>
                                    setStartDate(
                                        e.target.value,
                                    )
                                }
                            />

                        </div>

                        <div className="form-row">
                            <label>
                                適用終了日
                            </label>
                            <input
                                value={endDate}
                                readOnly
                            />

                        </div>

                        <MasterButtonArea
                            onCreate={handleCreate}
                            onUpdate={handleUpdate}
                            onDelete={handleDelete}
                            onClear={handleClear}
                            onBack={() =>
                                navigate(ROUTES.menu)
                            }
                            createDisabled={
                                !isCreateMode ||
                                !canCreate(
                                    qualificationName,
                                    qualificationAllowance,
                                    startDate)
                            }
                            updateDisabled={
                                !selected ||
                                !canUpdate(
                                    qualificationName,
                                    qualificationAllowance,
                                    startDate,
                                    endDate
                                )
                            }
                            deleteDisabled={
                                !selected ||
                                !canDeleteHistory(
                                    selected.startDate,
                                    selected.endDate,
                                )
                            }
                        />
                    </div>
                </div>
            </div>

            <div className="message-area">
                {message}
            </div>

        </div>
    );
}